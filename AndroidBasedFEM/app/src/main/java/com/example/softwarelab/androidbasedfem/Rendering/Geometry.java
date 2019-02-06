package com.example.softwarelab.androidbasedfem.Rendering;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class Geometry
{
    protected FloatBuffer vertexData;

    protected static final int BYTES_PER_FLOAT = 4;

    protected int color = 0;

    public Geometry()
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

    public abstract void setVertexData( float[] data );

    public abstract void Draw( Renderer renderer );

}
