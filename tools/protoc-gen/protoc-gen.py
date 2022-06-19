#!/usr/bin/env python3

# Generates BackendService.java from backend.proto
# This is the serialization mechanism/calling conventions for protobufs between rslib and rsdroid

import sys
import stringcase

from google.protobuf.compiler import plugin_pb2 as plugin

TYPE_ENUM = 14

# Needs map<> and Fluent import rather than Backend
ignore_methods_accepting = ["TranslateStringIn"]

def fix_namespace(f):
    return f.replace(".anki.", "anki.")


def basename(f):
    return f.split(".")[-1]


def proto_name_to_symbol(f):
    base = f.replace(".proto", "")
    return base.replace("anki/", "")

def proto_name_to_package(f):
    base = f.replace(".proto", "")
    head = base.replace("anki/", "anki.")
    return head

def get_annotation(type, optional=False):
    primitive = type not in [9, 12, 11, 14]
    if primitive:
        return ""
    elif optional:
        return "@Nullable"
    else:
        return "@NonNull"

def is_repeating(field):
    return field.label == 3

def as_getter(field):
    if is_repeating(field):
        return f"{field.name}List"
    else:
        return field.json_name

# https://developers.google.com/protocol-buffers/docs/reference/cpp/google.protobuf.descriptor
def to_java_type(type, field, output=False):
    ### Converts a given protobuf type to the Java Equivalent: (int, or List<Double> for example) ###
    primitive_list = {
        1: "Double",
        2: "Float",
        3: "Long",
        # 4 : "uint64",
        5: "Int",
        8: "Boolean",
        9: "String",
        12: "com.google.protobuf.ByteString",
        13: "Int",  # "uint32"
        17: "Int",
        18: "Long",
    }

    if is_repeating(field):
        primitive_map = {
            1: "Double",
            2: "Float",
            3: "Long",
            5: "Int",
            8: "Boolean",
            13: "Int",
        }
        if type != 14 and type != 11:
            new_type = (
                primitive_map[type]
                if type in primitive_map
                else primitive_list[type]
            )
        else:
            new_type = fix_namespace(field.type_name)

        if output:
            return "List<{}>".format(new_type)
        else:
            return "Iterable<{}>".format(new_type)

    if type == 14 or type == 11:  # enum/message
        return fix_namespace(field.type_name)

    return primitive_list[type]

class Message:
    def __init__(self, message, proto_file):
        self.method = message
        self.fields = message.field
        self.proto_file = proto_file



    def as_param(self, field):
        optional = getattr(field, "proto3_optional")
        annotation = "?" if optional else ""
        name = field.json_name
        if name == "val":
            name = "`val`"

        return "{}: {}{}".format(
           name, to_java_type(field.type, field), annotation, 
        )

    def as_setter_name(self, field):
        # We have a method: setXXX, so the first letter is uppercase
        return field[0].upper() + field[1:]

    def as_setter(self, field):
        optional = getattr(field, "proto3_optional", False)
        prefix = "set" if not is_repeating(field) else "addAll"
        name = field.json_name
        if name == "val":
            name = "`val`"

        return ".{}{}({})".format(
            prefix, self.as_setter_name(field.json_name), "it" if optional else name
        )

    def getFieldSetters(self):
        return [(self.as_setter(f), f) for f in self.fields]

    def is_primitive(self, field):
        return not is_repeating(field) and field.type not in [9, 12, 11, 14]

    def as_builder(self):
        # we can't set fields to null, so we can't use the builder fluent syntax.

        ret = "val builder = {namespace}.{methodName}.newBuilder();\n".format(
            methodName=self.method.name, namespace=self.proto_file.package
        )
        for setter, field in self.getFieldSetters():
            if not getattr(field, "proto3_optional", False): # self.is_primitive(field):
                ret += "    builder{};\n".format(setter)
            else:
                ret += "    {}?.let {{ builder{}; }}\n".format(
                    field.json_name, setter
                )

        ret += "    val input = builder.build();\n".format(
            self.proto_file.package, self.method.name
        )
        return ret.replace("I18n.", "I18N.")

    def as_params(self):
        return ", ".join([self.as_param(f) for f in self.fields])


class RPC:
    def __init__(self, service, service_index, command_num, messages, proto_file):
        self.method = service
        self.service_index = service_index
        self.command_num = command_num
        self.messages = messages
        self.proto_file = proto_file

    def is_valid(self):
        return self.get_input() and self.get_output()

    def parse(self, str, input):
        if input:
            return self.parse_input(str)
        else:
            return self.parse_output(str)

    @staticmethod
    def parse_input(str):
        if str == ".BackendProto.Empty":
            return "()"
        return "(" + fix_namespace(str) + " args)"

    @staticmethod
    def parse_output(str):
        str = fix_namespace(str)
        if str == "anki.generic.Empty":
            return "void"
        return str

    def get_input(self):
        return self.parse(self.method.input_type, True)

    def get_output(self):
        return self.parse(self.method.output_type, False)

    def as_command_name(self):
        return 'case {}: return "{}";'.format(self.command_num, self.method_name())

    def __repr__(self):
        input_type_name = fix_namespace(self.method.input_type)
        output_type_name = fix_namespace(self.method.output_type)
        input_msg = self.messages[input_type_name]
        out=self.get_output()
        name=self.method_name()
        inv="({})".format(self.messages[input_type_name].as_params())
        service=self.service_index
        method=self.command_num
        deser=self.messages[input_type_name].as_builder()

        if name in ("latestProgress", "syncMedia", "translateString"):
            raw_method = "runMethodRawNoLock"
        else:
            raw_method = "runMethodRaw"

        buf = f"""
@Throws(BackendException::class)
fun {name}Raw(input: ByteArray): ByteArray {{
    return {raw_method}({service}, {method}, input);
}}
"""

        if input_type_name == "anki.i18n.TranslateStringRequest":
            # maps not currently supported
            return buf

        if ((input_type_name.endswith("Request") or len(input_msg.fields) < 2) and not contains_oneof(input_msg)):
            # unroll input
            pass
        else:
            # skip unroll input
            inv=f"(input: {input_type_name})"
            deser = ""

        output_msg = self.messages[output_type_name]
        if (
            len(output_msg.fields) == 1
            and output_msg.fields[0].type != TYPE_ENUM
        ):
            # unwrap single return arg
            f = output_msg.fields[0]
            out = to_java_type(f.type, f, output=True)
            single_attribute = f".`{as_getter(f)}`"
        else:
            single_attribute = ""

        if out == "void":
            return_segment = f"""
{name}Raw(input.toByteArray());
            """
            out_with_colon = ""
        else:
            out_with_colon = f": {out}"
            return_segment = f"""\
try {{
    return {output_type_name}.parseFrom({name}Raw(input.toByteArray())){single_attribute};
}} catch (exc: com.google.protobuf.InvalidProtocolBufferException) {{
    throw BackendException("protobuf parsing failed");
}}"""

        buf += f"""
@Throws(BackendException::class)
open fun {name}{inv}{out_with_colon} {{
    {deser}
    {return_segment}
}}
            """

        return buf

    def method_name(self):
        return self.method.name[0].lower() + self.method.name[1:]


def gather_classes(proto_file, all_messages, service_index):
    classes = []

    for f in proto_file.service:
        for i, m in enumerate(f.method):
            cls = RPC(m, service_index, i, all_messages, proto_file)
            if not cls.is_valid():
                raise ValueError(str(m))
            classes.append(cls)

    return classes


def logRepr(s):
    sys.stderr.write("\n".join(dir(s)))


def log(*args):
    print(*args, file=open("/tmp/log.txt", "a"))


def generate_code(request, response):
    # gather all messages and service indexes
    all_messages = {}
    service_index = {}
    for proto_file in request.proto_file:
        for message in proto_file.message_type:
            all_messages[f"{proto_file.package}.{message.name}"] = Message(message, proto_file)
        for enum in proto_file.enum_type:
            if enum.name == "ServiceIndex":
                for value in enum.value:
                    pkg = value.name.replace("SERVICE_INDEX_", "").lower()
                    if pkg == "deck_config":
                        pkg = "deckconfig"
                    service_index[f"anki.{pkg}"] = value.number

    file_contents = [
        """
/* Auto-generated from the .proto files in AnkiDroidBackend. */

@file:Suppress("NAME_SHADOWING")

package anki.backend;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.GeneratedMessageV3;

import net.ankiweb.rsdroid.BackendException;

public abstract class GeneratedBackend {

@Throws(BackendException::class)
protected abstract fun runMethodRaw(service: Int, method: Int, input: ByteArray): ByteArray;
@Throws(BackendException::class)
protected abstract fun runMethodRawNoLock(service: Int, method: Int, input: ByteArray): ByteArray;

"""
    ]

    for proto_file in request.proto_file:
        if not len(proto_file.service):
            continue

        service_methods = gather_classes(proto_file, all_messages, service_index[proto_file.package])
        if not service_methods:
            continue

        for method in service_methods:
            file_contents.append("\n\n" + str(method))

    file_contents.append("\n}")
    f = response.file.add()
    f.name = "GeneratedBackend.kt"
    f.content = "\n".join(file_contents)

def contains_oneof(msg):
    for field in msg.fields:
        if field.oneof_index:
            return True
    return False

if __name__ == "__main__":
    # Read request message from stdin
    data = sys.stdin.buffer.read()

    # Parse request
    request = plugin.CodeGeneratorRequest()
    request.ParseFromString(data)

    # Create response
    response = plugin.CodeGeneratorResponse()
    # fixme: check these are actually being handled
    response.supported_features |= plugin.CodeGeneratorResponse.FEATURE_PROTO3_OPTIONAL

    # Generate code
    generate_code(request, response)

    # Serialise response message
    output = response.SerializeToString()

    # Write to stdout
    sys.stdout.buffer.write(output)
