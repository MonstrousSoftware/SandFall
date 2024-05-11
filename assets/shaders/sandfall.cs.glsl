/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
#version 430 core
#pragma {{DEFINES}}

layout (local_size_x=WX, local_size_y=WY) in;

layout(binding = 0, rgba8) uniform readonly restrict image2D readImage;
layout(binding = 1, rgba8) uniform writeonly restrict image2D writeImage;

uniform int u_step; // frame counter

const float bottom = gl_NumWorkGroups.y * gl_WorkGroupSize.y - 120.0;    // y-coordinate of "ground surface"


// a black pixel is counted as empty space
bool isEmpty(vec4 color){
    return color.r == 0 && color.g == 0 && color.b == 0;
}

// read pixel from the input buffer
vec4 readPixel(uint x, uint y){
    return imageLoad(readImage, ivec2(x, y)).rgba;
}

void slide_left( uvec2 pos ){
    vec4 pixel = readPixel(pos.x, pos.y);
    vec4 aboveRight = readPixel(pos.x+1, pos.y-1);
    vec4 right = readPixel(pos.x+1, pos.y);
    vec4 below = readPixel(pos.x, pos.y+1);
    vec4 belowLeft = readPixel(pos.x-1, pos.y+1);

    if(pos.y <= bottom) {
        if (pos.y > 0 && isEmpty(pixel) && !isEmpty(right) && !isEmpty(aboveRight)) {
            pixel = aboveRight;
        }
        else if (pos.y < bottom && pos.x > 0 && !isEmpty(pixel) && !isEmpty(below) && isEmpty(belowLeft) ) {
            pixel = belowLeft;
        }
    }
    // write  pixel
    imageStore(writeImage, ivec2(gl_GlobalInvocationID.xy), pixel);
}


void slide_right( uvec2 pos ){
    vec4 pixel = readPixel(pos.x, pos.y);
    vec4 aboveLeft = readPixel(pos.x-1u, pos.y-1u);
    vec4 left = readPixel(pos.x-1u, pos.y);
    vec4 below = readPixel(pos.x, pos.y+1u);
    vec4 belowRight = readPixel(pos.x+1u, pos.y+1u);

    if(pos.y <= bottom) {
        if (pos.y > 0 && isEmpty(pixel) && !isEmpty(left) && !isEmpty(aboveLeft)) {
            pixel = aboveLeft;
        }
        else if (pos.y < bottom && pos.x < WX && !isEmpty(pixel) && !isEmpty(below) && !isEmpty(belowRight) ) {
            pixel = belowRight;
        }
    }
    // write  pixel
    imageStore(writeImage, ivec2(gl_GlobalInvocationID.xy), pixel );
}

bool drop_down( uvec2 pos ){
    vec4 pixel = readPixel(pos.x, pos.y);
    vec4 above = readPixel(pos.x, pos.y-1);
    vec4 below = readPixel(pos.x, pos.y+1);
    bool moved = false;

    if(pos.y <= bottom) {
        // if the current pixel is empty and the one above is not, adopt the colour from the above pixel
        if (pos.y > 0 && isEmpty(pixel) && !isEmpty(above)) {
            pixel = above;
            moved = true;
        }
        // if the pixel is not empty and the one below is, adopt the colour from the empty space
        else if (pos.y < bottom && isEmpty(below) && !isEmpty(pixel)) {
            pixel = below;
            moved = true;
        }
    }
    // write  pixel
    imageStore(writeImage, ivec2(gl_GlobalInvocationID.xy), pixel);
    return moved;
}


void main(void) {
    uvec2 pos = gl_GlobalInvocationID.xy;

    if(!drop_down(pos)) {
        // slide left or right on alternate frames
        if ((u_step % 2) == 0)
            slide_left(pos);
        else
            slide_right(pos);
    }

}
