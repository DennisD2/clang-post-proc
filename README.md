# clang-post-proc
C Language Post Processor

This project takes a C struct as input and can replace Retdec C source 
code lines like
```C
(a1 + 128)
```

With correct struct information like
```C
&(a1->some_attribute)
```

# Build
```bash
mvn clean install
```

# Use
See pom.xml, profile ```clang```.

```bash
mvn clean install -P clang
```

Plugin execution id ```create-sed-commands``` creates a file with
sed commands that replaces address offset operations with struct uses.

Plugin execution id ```augment-clang-source``` applies the sed
command file created in the step above (using sed) to a C source file.

Result files (sed command file and augmented C source) are created in
```target```.

# Requirements
Plugin execution id ```augment-clang-source``` requires ```bash``` and 
```sed```. 

# Restrictions
Currently only a single can be handled at a time.

Data type sizes are burned into the code.
Check ClangGenerator class which contains hardcoded values for 
dta type sizes (e.g. int16_t, int, ...). These values vary and need to
be adapted to other CPUs.



