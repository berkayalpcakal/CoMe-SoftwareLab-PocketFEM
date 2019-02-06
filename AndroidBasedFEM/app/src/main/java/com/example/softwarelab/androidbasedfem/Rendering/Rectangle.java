package com.example.softwarelab.androidbasedfem.Rendering;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Rectangle extends Geometry
{

    // Empty Constructor
    public Rectangle()
    {
    }

    public void setVertexData( float[] vertices )
    {
        float[] tableVertices = {

                // Rectangle 1 -- vertices aligned clockwise
                vertices[0], vertices[1],

                vertices[0], vertices[3],

                vertices[2], vertices[3],

                vertices[2], vertices[1] };

        vertexData = ByteBuffer
                .allocateDirect(tableVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        vertexData.put(tableVertices);
    }

    public void Draw( Renderer renderer )
    {
        renderer.Draw( vertexData, color, 4,0, true );
        renderer.Draw( vertexData, 1 , 4,0, false );
    }
}
