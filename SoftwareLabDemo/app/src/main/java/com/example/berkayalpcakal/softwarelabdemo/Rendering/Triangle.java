package com.example.berkayalpcakal.softwarelabdemo.Rendering;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Triangle extends Geometry
{

    // Empty Constructor
    public Triangle()
    {
    }

    public void setVertexData( float[] vertices )
    {
        float[] tableVertices = {

                vertices[0], vertices[1],

                vertices[2], vertices[3],

                vertices[4], vertices[5]
        };

        vertexData = ByteBuffer

                .allocateDirect(tableVertices.length * BYTES_PER_FLOAT)

                .order(ByteOrder.nativeOrder())

                .asFloatBuffer();
        vertexData.put(tableVertices);
    }

    public void Draw( Renderer renderer )
    {
        renderer.Draw( vertexData, color, 3 );
    }
}
