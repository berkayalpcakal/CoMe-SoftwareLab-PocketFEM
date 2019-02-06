package com.example.softwarelab.androidbasedfem.Rendering;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class RollerSupport extends BoundaryCondition
{
    // Empty Constructor
    public RollerSupport()
    {
    }

    public void setVertexData( float[] _point, int _direction, float _factor, float _minDim ) {

        direction = _direction;
        point = _point;
        updateVertexData( _factor, _minDim  );


        color = 1;
    }

    public void updateVertexData( float _factor, float minDim )
    {
        // Limits the size the roller support
        float factor = Math.max(0, Math.min(_factor, minDim*1.0f));
        // Horizontal
        if (direction == 0) {
            float[] tableVertices1 = {

                    // Arrow vertices
                    point[0], point[1],

                    point[0] - 0.2f*factor, point[1] - 0.2f*factor,

                    point[0] - 0.2f * factor, point[1] + 0.2f*factor,

                    point[0] - 0.25f*factor, point[1] - 0.2f*factor,

                    point[0] - 0.25f*factor, point[1] + 0.2f*factor };

            vertexData = ByteBuffer
                    .allocateDirect(tableVertices1.length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            vertexData.put(tableVertices1);
        } else
        {   // Vertical
            float[] tableVertices2 = {

                    // Arrow vertices
                    point[0], point[1],

                    point[0] + 0.2f*factor, point[1] - 0.2f*factor,

                    point[0] - 0.2f*factor, point[1] - 0.2f*factor,

                    point[0] + 0.2f*factor, point[1] - 0.25f*factor,

                    point[0] - 0.2f*factor, point[1] - 0.25f*factor };


            vertexData = ByteBuffer
                    .allocateDirect(tableVertices2.length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            vertexData.put(tableVertices2);
        }


    }

    public void Draw( Renderer renderer )
    {
        renderer.Draw( vertexData, color, 3, 0, true );
        renderer.Draw( vertexData, 2, 3, 0, false );
        renderer.Draw( vertexData, 2, 2, 3, false );
    }
}