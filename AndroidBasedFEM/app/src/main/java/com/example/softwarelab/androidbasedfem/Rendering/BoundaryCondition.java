package com.example.softwarelab.androidbasedfem.Rendering;

import java.nio.FloatBuffer;

public abstract class BoundaryCondition
{
    protected FloatBuffer vertexData;

    protected static final int BYTES_PER_FLOAT = 4;

    protected int color = 0;

    protected int direction;

    protected float[] point;

    public BoundaryCondition()
    {
    }

    public void changeColor( int color_ )
    {
        color = color_;
    }

    public FloatBuffer getVertices()
    {
        return vertexData;
    }

    public abstract void setVertexData( float[] data, int _direction, float _factor, float _minDim );

    public abstract void updateVertexData( float factor, float minDim );

    public abstract void Draw( Renderer renderer );

}
