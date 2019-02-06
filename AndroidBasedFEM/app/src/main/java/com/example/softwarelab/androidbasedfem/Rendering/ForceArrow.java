package com.example.softwarelab.androidbasedfem.Rendering;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class ForceArrow extends BoundaryCondition
{

    // Empty Constructor
    public ForceArrow()
    {
    }

    public void setVertexData( float[] _point, int _direction, float _factor, float _minDim )
    {
        point = _point;
        direction = _direction;
        updateVertexData(_factor,_minDim);
        // red
        color = 2;
    }

    public void updateVertexData( float _factor, float minDim )
    {
        float factor = Math.max(0, Math.min(_factor, minDim * 1.0f));
        // Vertical Arrow
        if( direction == 1) {

            float[] tableVertices = {

                    // Arrow vertices
                    point[0] - 0.03f * factor, point[1] + 0.2f * factor,

                    point[0] + 0.03f * factor, point[1] + 0.2f * factor,

                    point[0] + 0.03f * factor, point[1] + (0.5f) * factor,

                    point[0] - 0.03f * factor, point[1] + (0.5f) * factor,

                    point[0], point[1],

                    point[0] - 0.2f * factor, point[1] + 0.2f * factor,

                    point[0] + 0.2f * factor, point[1] + 0.2f * factor,

                    point[0], point[1]};

            vertexData = ByteBuffer
                    .allocateDirect(tableVertices.length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            vertexData.put(tableVertices);
        } else
        {   // Horizontal Arrow
            float[] tableVertices = {

                    // Arrow vertices
                    point[0] - 0.2f * factor, point[1] - 0.03f * factor,

                    point[0] - 0.2f * factor, point[1] + 0.03f * factor,

                    point[0] - (0.5f) * factor, point[1] + 0.03f * factor,

                    point[0] - (0.5f) * factor, point[1] - 0.03f * factor,

                    point[0], point[1],

                    point[0] - 0.2f * factor, point[1] - 0.2f * factor,

                    point[0] - 0.2f * factor, point[1] + 0.2f * factor,

                    point[0], point[1]};

            vertexData = ByteBuffer
                    .allocateDirect(tableVertices.length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            vertexData.put(tableVertices);
        }

    }

    public void Draw( Renderer renderer )
    {

        renderer.Draw( vertexData, color, 4, 4, true );
        renderer.Draw( vertexData, color, 4, 0, true );
    }
}