ls -l

SO=$(find . -name "*.so" | wc -l)
DYLIB=$(find . -name "*.dylib" | wc -l)
DLL=$(find . -name "*.dll" | wc -l)
if [ $SO -ne 1 ] 
then 
    echo "Error: Expected 1 .so file"; 
	exit 1; 
fi

if [ $DYLIB -ne 1 ]
then 
    echo "Error: Expected 1 .dylib file" 
	exit 1
fi
if [ $DLL -ne 1 ]
then 
	echo "Error: Expected 1 .dll file" 
	exit 1
fi
