#!/usr/bin/env python

# Generates BackendService.java from backend.proto
# This is the serialization mechanism/calling conventions for protobufs between rslib and rsdroid

import sys

from google.protobuf.compiler import plugin_pb2 as plugin

# Needs map<> and Fluent import rather than Backend
ignore_methods_accepting = ["TranslateStringIn"]

class Method:
    def __init__(self, method):
        self.method = method
        self.fields = method.field


    def to_java_type(self, type, field):
        ### Converts a given protobuf type to the Java Equivalent: (int, or List<Double> for example) ###
        primitive_list = {
                1: "double",
                2 : "float",
                3 : "long",
                # 4 : "uint64",
                5 : "int",
                8 : "boolean",
                9 : "java.lang.String",
                12 : "com.google.protobuf.ByteString",
                13: "int" #"uint32"
                 }

        def fix_namespace(f):
            return f.replace(".BackendProto.", "Backend.")


        if self.is_repeating(field):
            primitive_map = {1 : "Double",
                        2 : "Float",
                        3 : "Long",
                        5 : "Integer",
                        8 : "Boolean",
                        13: "Integer"}
            if type != 14 and type != 11:
                new_type = primitive_map[type] if type in primitive_map else primitive_list[type]
            else:
                new_type = fix_namespace(field.type_name)
            return "java.util.List<{}>".format(new_type)

        if type == 14 or type == 11: # enum/message
            return fix_namespace(field.type_name)

        return primitive_list[type]

    def label_to_annotation(self, label):
        return {
            1: "@Nullable ", # LABEL_OPTIONAL
            3: ""            # LABEL_REPEATED
        }[label]
    def as_param(self, field):
        annotation = self.label_to_annotation(field.label)
        # avoid annotations for primitives
        if field.type not in [9, 12, 11, 14]:
            annotation = ""
        return "{}{} {}".format(annotation, self.to_java_type(field.type, field), field.json_name)

    def is_repeating(self, field):
        return field.label == 3

    def as_setter_name(self, field):
        # We have a method: setXXX, so the first letter is uppercase
        return field[0].upper() + field[1:]

    def as_setter(self, field):
        prefix = "set" if not self.is_repeating(field) else "addAll"
        return ".{}{}({})".format(prefix, self.as_setter_name(field.json_name), field.json_name)

    def getFieldSetters(self):
        return [(self.as_setter(f), f) for f in self.fields]

    def is_primitive(self, field):
        return not self.is_repeating(field) and field.type not in [9, 12, 11, 14]

    def as_builder(self):
        # we can't set fields to null, so we can't use the builder fluent syntax.

        ret = "Backend.{}.Builder builder = Backend.{}.newBuilder();\n".format(self.method.name, self.method.name)
        for setter, field in self.getFieldSetters():
            if self.is_primitive(field):
                ret += "            builder{};\n".format(setter)
            else:
                ret += "            if ({} != null) {{ builder{}; }}\n".format(field.json_name, setter)


        ret += "            Backend.{} protobuf = builder.build();\n".format(self.method.name)
        return ret


    def as_params(self):
        return ", ".join([self.as_param(f) for f in self.fields])

class RPC:
    def __init__(self, service, command_num, methods):
        self.method = service
        self.command_num = command_num
        self.method_lookup = methods

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
        return "(" + str.replace(".BackendProto.", "Backend.") + " args)"

    @staticmethod
    def parse_output(str):
        if str == ".BackendProto.Empty":
            return "void"
        return str.replace(".BackendProto.", "Backend.")

    def get_input(self):
        return self.parse(self.method.input_type, True)

    def get_output(self):
        return self.parse(self.method.output_type, False)
        
    def as_interface(self):
        if self.get_output() == "void":
            k = self.method.input_type.replace(".BackendProto.", "")
            if k in self.method_lookup:
                return "    {out} {name}{inv} throws BackendException;".format(out=self.get_output(), name=self.method_name(), inv="({})".format(self.method_lookup[k].as_params()))
            else:
                return "    {out} {name}{inv} throws BackendException;".format(out=self.get_output(), name=self.method_name(), inv=self.get_input())
        else:
            k = self.method.input_type.replace(".BackendProto.", "")
            if k in self.method_lookup:
                return "    {out} {name}{inv} throws BackendException;".format(out=self.get_output(), name=self.method_name(), inv="({})".format(self.method_lookup[k].as_params()))
            else:
                return "    {out} {name}{inv} throws BackendException;".format(out=self.get_output(), name=self.method_name(), inv=self.get_input())

    def __repr__(self):
        args = "args.toByteArray()" if self.get_input() != "()" else "Backend.Empty.getDefaultInstance().toByteArray()"
        # These previously were very different - validation changed this.
        # Might want to merge these branches now they're so similar.

        if self.get_output() == "void":
            k = self.method.input_type.replace(".BackendProto.", "")
            if k in self.method_lookup:
                return "    public {out} {name}{inv} throws BackendException {{ \n" \
                       "        try {{\n" \
                       "            {deser}\n" \
                       "            Pointer backendPointer = ensureBackend();\n" \
                       "            byte[] result = NativeMethods.command(backendPointer.toJni(), {num}, protobuf.toByteArray());\n" \
                       "            Backend.Empty message = Backend.Empty.parseFrom(result);\n" \
                       "            validateMessage(result, message);\n" \
                       "        }} catch (InvalidProtocolBufferException ex) {{\n" \
                       "            throw new BackendException(ex);\n" \
                       "        }}\n" \
                       "    }}".format(out=self.get_output(), name=self.method_name(), inv="({})".format(self.method_lookup[k].as_params()),
                                       num=self.command_num,
                                       deser=self.method_lookup[k].as_builder())
            else:
                return "    public {out} {name}{inv} throws BackendException {{ \n" \
                       "        try {{\n" \
                       "            Pointer backendPointer = ensureBackend();\n" \
                       "            byte[] result = NativeMethods.command(backendPointer.toJni(), {num}, {args});\n" \
                       "            Backend.Empty message = Backend.Empty.parseFrom(result);\n" \
                       "            validateMessage(result, message);\n" \
                       "        }} catch (InvalidProtocolBufferException ex) {{\n" \
                       "            throw new BackendException(ex);\n" \
                       "        }}\n" \
                       "    }}".format(out=self.get_output(), name=self.method_name(), inv=self.get_input(),
                                       num=self.command_num,
                                       args=args)
        else:
            k = self.method.input_type.replace(".BackendProto.", "")
            if k in self.method_lookup:
                return "    public {out} {name}{inv} throws BackendException {{ \n" \
                       "        try {{\n" \
                       "            {deser}\n" \
                       "            Pointer backendPointer = ensureBackend();\n" \
                       "            byte[] result = NativeMethods.command(backendPointer.toJni(), {num}, protobuf.toByteArray());\n" \
                       "            {out} message = {out}.parseFrom(result);\n" \
                       "            validateMessage(result, message);\n" \
                       "            return message;\n" \
                       "        }} catch (InvalidProtocolBufferException ex) {{\n" \
                       "            throw new BackendException(ex);\n" \
                       "        }}\n" \
                       "    }}".format(out=self.get_output(), name=self.method_name(), inv="({})".format(self.method_lookup[k].as_params()),
                                       num=self.command_num,
                                       deser=self.method_lookup[k].as_builder())
            else:
                # Definitely empty - and TranslateStringIn (manually ignored)
                return "    public {out} {name}{inv} throws BackendException {{ \n" \
                       "        try {{\n" \
                       "            Pointer backendPointer = ensureBackend();\n" \
                       "            byte[] result = NativeMethods.command(backendPointer.toJni(), {num}, {args});\n" \
                       "            {out} message = {out}.parseFrom(result);\n" \
                       "            validateMessage(result, message);\n" \
                       "            return message;\n" \
                       "        }} catch (InvalidProtocolBufferException ex) {{\n" \
                       "            throw new BackendException(ex);\n" \
                       "        }}\n" \
                       "    }}".format(out=self.get_output(), name=self.method_name(), inv=self.get_input(),
                                       num=self.command_num,
                                       args=args)

    def method_name(self):
        return self.method.name[0].lower() + self.method.name[1:]


def traverse(proto_file):
    classes = []
    methods = [Method(m) for m in proto_file.message_type if m.name.endswith("In")]

    method_lookup = {item.method.name: item for item in set(methods) if item.method.name not in ignore_methods_accepting}

    for f in proto_file.service:
        for i, m in enumerate(f.method):
            cls = RPC(m, i + 1, method_lookup)
            if not cls.is_valid():
                raise ValueError(str(m))
            classes.append(cls)

    return classes

def logRepr(s):
    sys.stderr.write("\n".join(dir(s)))

def log(s):
    sys.stderr.write(str(s) + "\n")


def generate_code(request, response):
    for proto_file in request.proto_file:

        service_methods = traverse(proto_file)
        if not service_methods:
            continue

        class_name = proto_file.name.capitalize().replace(".proto", "").replace("Backend", "RustBackend")
        file_contents = ["/*\n "
                         "  This class was autogenerated from {} by {}\n"
                         "  Please Rebuild project to regenerate."
                         " */\n\n"
                         "package net.ankiweb.rsdroid;\n\n"
                         "import androidx.annotation.Nullable;\n\n"
                         "import com.google.protobuf.InvalidProtocolBufferException;\n"
                         "import com.google.protobuf.GeneratedMessageV3;\n\n"
                         "import BackendProto.Backend;\n\n"
                         "public abstract class {cls}Impl implements net.ankiweb.rsdroid.{cls} {{\n\n"
                         "    public abstract Pointer ensureBackend();\n\n\n"
                         "    protected void validateMessage(byte[] result, GeneratedMessageV3 message) throws BackendException, InvalidProtocolBufferException {{\n"
                         "        if (message.getUnknownFields().asMap().isEmpty()) {{\n"
                         "            return;\n"
                         "        }}\n"
                         "        Backend.BackendError ex = Backend.BackendError.parseFrom(result);\n"
                         "        throw new BackendException(ex.getLocalized());\n"
                         "    }}".format(proto_file.name, __file__, cls=class_name)]

        for method in service_methods:
            file_contents.append("\n\n" + str(method))
            
        file_contents.append("\n}")

        # Fill response
        f = response.file.add()
        f.name = class_name + "Impl.java"
        f.content = "\n".join(file_contents)
        
        # generate interface (if methods)
        
        iface_contents = ["/*\n "
                         "  This class was autogenerated from {} by {}\n"
                         "  Please Rebuild project to regenerate."
                         " */\n\n"
                         "package net.ankiweb.rsdroid;\n\n"
                         "import androidx.annotation.Nullable;\n\n"
                         "import BackendProto.Backend;\n\n"
                         "public interface {} {{".format(proto_file.name, __file__, class_name)]
        for method in service_methods:
            iface_contents.append("\n" + str(method.as_interface()))
        iface_contents.append("\n}")
        iface = response.file.add()
        iface.name = class_name + ".java"
        iface.content = "\n".join(iface_contents)


if __name__ == '__main__':
    # Read request message from stdin
    data = sys.stdin.buffer.read()

    # Parse request
    request = plugin.CodeGeneratorRequest()
    request.ParseFromString(data)

    # Create response
    response = plugin.CodeGeneratorResponse()

    # Generate code
    generate_code(request, response)

    # Serialise response message
    output = response.SerializeToString()

    # Write to stdout
    sys.stdout.buffer.write(output)
