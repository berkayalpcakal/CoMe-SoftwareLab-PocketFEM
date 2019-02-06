package com.example.softwarelab.androidbasedfem.Rendering;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class colorBar {

    protected FloatBuffer vertexData;

    protected FloatBuffer colorData;

    private float screenHeight;

    int BYTES_PER_FLOAT = 4;

    // Empty Constructor
    public colorBar( float xOffset, float yOffset, float viewScale, float height  ) {

        screenHeight = height;
        updateColorBar( xOffset, yOffset, viewScale );

        // Colors are blue, green, red
        float[] tableColors = {

                0.0f, 0.0f, 1.0f, 1.0f,

                0.0f, 1.0f, 0.0f, 1.0f,

                1.0f, 0.0f, 0.0f, 1.0f };

        colorData = ByteBuffer

                .allocateDirect(tableColors.length * BYTES_PER_FLOAT)

                .order(ByteOrder.nativeOrder())

                .asFloatBuffer();

        colorData.put(tableColors);
    }

    public void updateColorBar(float xOffset, float yOffset, float viewScale ) {

        float[] tableVertices = {

                (-0.8f + xOffset) * viewScale, (0.9f*screenHeight + yOffset) *viewScale,

                ( xOffset) * viewScale, (0.9f*screenHeight + yOffset) *viewScale,

                (0.8f + xOffset) * viewScale, (0.9f*screenHeight + yOffset) *viewScale };

        vertexData = ByteBuffer

                .allocateDirect(tableVertices.length * BYTES_PER_FLOAT)

                .order(ByteOrder.nativeOrder())

                .asFloatBuffer();

        vertexData.put(tableVertices);
    }

    public void DrawColorBar( Renderer renderer )
    {

        renderer.DrawC(vertexData, colorData );

    }
}
