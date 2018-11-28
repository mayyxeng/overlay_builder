Overlay Builder Project
=======================

The goal of this project is to ease elastic overlay generation. It is somehow a back-end for the whole elastic overlay project.
This project was initialized using the **Chisel Project Template** found on [Chisel3](https://github.com/freechipsproject/chisel3) project.
All you can find the `overlay` package in `src` directory. Some `modules` have `Scala` test benches. It is a work in progress.

If you haven't worked with `Chisel` before, head to this [tutorial](https://github.com/ucb-bar/chisel-tutorial).

Every `module` has an `object` for `Verilog` generation. To generate `Verilog` simply do the following from project's parent directory:
```bash
sbt 'runMain overlay.$MODULE_NAME $ARGS'
```
where `$MODULE_NAME` is name of the module and `$ARGS` is arguments to pass to `Chisel` Driver. You can see a list of option by passing `--help` as the `$ARGS` list.

if the module has `Scala` test associated with it you can run the test using
```bash
sbt `testOnly overlay.${Module_Name}Spec`
```

### License
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org/>
