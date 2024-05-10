/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
#version 430 core
#pragma {{DEFINES}}

layout (local_size_x=WX, local_size_y=WY) in;

layout(binding = 0, rgba8) uniform readonly restrict image2D readImage;
layout(binding = 1, rgba8) uniform writeonly restrict image2D writeImage;

uniform int u_step;

const int bottom = 480;




void slide_left( uvec2 pos ){
    vec4 pixel = imageLoad(readImage, ivec2(pos.xy)).rbga;
    vec4 aboveRight = imageLoad(readImage, ivec2(pos.x+1, pos.y-1)).rbga;
    vec4 right = imageLoad(readImage, ivec2(pos.x+1, pos.y)).rbga;
    vec4 below = imageLoad(readImage, ivec2(pos.x, pos.y+1)).rbga;
    vec4 belowLeft = imageLoad(readImage, ivec2(pos.x-1, pos.y+1)).rbga;

    if(pos.y <= bottom) {
        if (pos.y > 0 && pixel.r == 0 && right.r != 0 && aboveRight.r != 0) {
            pixel = aboveRight;
        }
        else if (pos.y < bottom && pos.x > 0 && pixel.r != 0 && below.r != 0 && belowLeft.r == 0 ) {
            pixel = belowLeft;
        }
    }
    // write  pixel
    imageStore(writeImage, ivec2(gl_GlobalInvocationID.xy), pixel);
}


void slide_right( uvec2 pos ){
    vec4 pixel = imageLoad(readImage, ivec2(pos.xy)).rbga;
    vec4 aboveLeft = imageLoad(readImage, ivec2(pos.x-1, pos.y-1)).rbga;
    vec4 left = imageLoad(readImage, ivec2(pos.x-1, pos.y)).rbga;
    vec4 below = imageLoad(readImage, ivec2(pos.x, pos.y+1)).rbga;
    vec4 belowRight = imageLoad(readImage, ivec2(pos.x+1, pos.y+1)).rbga;

    if(pos.y <= bottom) {
        if (pos.y > 0 && pixel.r == 0 && left.r != 0 && aboveLeft.r != 0) {
            pixel = aboveLeft;
        }
        else if (pos.y < bottom && pos.x < WX && pixel.r != 0 && below.r != 0 && belowRight.r == 0 ) {
            pixel = belowRight;
        }
    }
    // write  pixel
    imageStore(writeImage, ivec2(gl_GlobalInvocationID.xy), pixel);
}

bool drop_down( uvec2 pos ){
    bool moved = false;
    vec4 pixel = imageLoad(readImage, ivec2(pos.xy)).rbga;
    vec4 above = imageLoad(readImage, ivec2(pos.x, pos.y-1)).rbga;
    vec4 below = imageLoad(readImage, ivec2(pos.x, pos.y+1)).rbga;

    if(pos.y <= bottom) {
        if (pos.y > 0 && pixel.r == 0 && above.r != 0) {
            pixel = above;
            moved = true;
        }
        else if (pos.y < bottom && below.r == 0 && pixel.r != 0) {
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
        int action = (u_step % 2);
        if (action == 0)
        slide_left(pos);
        else
        slide_right(pos);
    }
//    else
//        drop_down(pos);
}
