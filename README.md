# SandFall

Demonstrator of a falling sand effect using compute shaders.

Requires GLES 3.1.

![screenshot](https://github.com/MonstrousSoftware/SandFall/assets/49096535/9af80de3-b02e-4f52-aa6d-6414b9c3ff34)


How does it work?
Two textures are used as input buffer and output buffer for a compute shader.  
The compute shader executes a falling sand algorithm on each pixel from the input buffer
and writes the result to the output buffer.  Black pixels are considered empty and coloured
pixels will seem to fall in the empty space.


## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3.
