# SandFall

Demonstrator of a falling sand effect using compute shaders.

Requires GLES 3.1.

Controls:
- mouse drag to add sand
- SPACE to freeze animation
- S to single-step when frozen
- R to reset

![screenshot](https://github.com/MonstrousSoftware/SandFall/assets/49096535/9af80de3-b02e-4f52-aa6d-6414b9c3ff34)


How does it work?
Two textures are used as input buffer and output buffer for a compute shader.  
The compute shader executes a falling sand algorithm on each pixel from the input buffer
and writes the result to the output buffer.  
As this is a compute shader, the GPU runs the calculation for each pixel in parallel.

Black pixels are considered empty and coloured pixels will seem to fall in the empty space.
The compute shader (see: sandfall.cs.glsl) is coded in GLSL and acts as a cellular automaton.
To avoid racing conditions, each shader instantiation will only write to its own pixel position.
For example: if a pixel is empty (black) and the pixel above is not (another colour), then the 
output pixel will take the colour from the above pixel to simulate the colour dropping down.
Meanwhile, the shader for the pixel above detects it has a colour and the cell below is empty,
so it will output an empty colour.



## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3.
