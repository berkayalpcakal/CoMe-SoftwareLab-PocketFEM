package com.example.softwarelab.androidbasedfem.Rendering;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Triangle extends Geometry
{
    protected FloatBuffer colorData;

    protected int[] EFT = new int[6];

    protected float[] StaticDisp = new float[6];

    // Strain and stress hold sx, sy and sxy (Not vsiualized yet!! Can be added to app)
    protected float[] strain = new float[3];
    protected float[] stress = new float[3];

    // Averaged vonMises stresses and strains
    protected float[] vonMisesStressAveragedToNodes = new float[3];
    protected float[] vonMisesStrainAveragedToNodes = new float[3];

    protected float vonMises = 0f;
    protected float vonMisesStrain = 0.0f;

    private int pos;

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

    public void setColorData( int field, float globalMaxValue, float globalMinValue )
    {
        float value1 = 0.0f;
        float value2 = 0.0f;
        float value3 = 0.0f;

        if ( field == 0 )
        {
            value1 =  (float) Math.sqrt( Math.pow( StaticDisp[0],2) +  Math.pow( StaticDisp[1],2) );
            value2 =  (float) Math.sqrt( Math.pow( StaticDisp[2],2) +  Math.pow( StaticDisp[3],2));
            value3 =  (float) Math.sqrt( Math.pow( StaticDisp[4],2) +  Math.pow( StaticDisp[5],2));
        }

        if (field == 1)
        {
            value1 =  vonMisesStressAveragedToNodes[0];
            value2 =  vonMisesStressAveragedToNodes[1];
            value3 =  vonMisesStressAveragedToNodes[2];
        }

        if (field == 2)
        {
            value1 =  vonMisesStrainAveragedToNodes[0];
            value2 =  vonMisesStrainAveragedToNodes[1];
            value3 =  vonMisesStrainAveragedToNodes[2];
        }


        // Map Displacement to a value between 0 and 1
        float OldRange = globalMaxValue - globalMinValue;
        float half = OldRange/2.0f;
        float NewRangeSmall = (1.0f - 0.0f);

        float color1 = (((value1 - globalMinValue) * NewRangeSmall) / OldRange) + 0.0f;
        float color2 = (((value2 - globalMinValue) * NewRangeSmall) / OldRange) + 0.0f;
        float color3 = (((value3 - globalMinValue) * NewRangeSmall) / OldRange) + 0.0f;

        // The map again 0-0.5 to a value of 0-1, if value smaller then "half" ( blue -> green)
        // And 0.5-1 to a value of 0-1, if value bigger then 2half"            (green -> red)
        // red   1,0,0,1
        // blue  0,0,1,1
        // green 0,1,0,1
        float[] color1Array = {0.0f, 0.0f, 0.0f, 1.0f };
        if ( value1 < half )
        {
            color1Array[1] = color1/0.5f;
            color1Array[2] = 1.0f-color1/0.5f;
        } else
        {
            color1Array[0] = ( color1 - 0.5f) / 0.5f;
            color1Array[1] = 1.0f- ( color1 - 0.5f)/0.5f;
        }
        float[] color2Array = {0.0f, 0.0f, 0.0f, 1.0f };
        if ( value2 < half )
        {
            color2Array[1] = color2/0.5f;
            color2Array[2] = 1.0f- color2/0.5f;
        } else
        {
            color2Array[0] = ( color2 - 0.5f ) / 0.5f;
            color2Array[1] = 1.0f- ( color2 - 0.5f ) / 0.5f;
        }
        float[] color3Array = {0.0f, 0.0f, 0.0f, 1.0f };
        if ( value3 < half )
        {
            color3Array[1] = color3/0.5f;
            color3Array[2] = 1.0f-color3/0.5f;
        } else
        {
            color3Array[0] = ( color3 - 0.5f ) / 0.5f;
            color3Array[1] = 1.0f- ( color3 - 0.5f) / 0.5f;
        }

        float tmpColor[] = {
                color1Array[0], color1Array[1], color1Array[2], color1Array[3],
                color2Array[0], color2Array[1], color2Array[2], color2Array[3],
                color3Array[0], color3Array[1], color3Array[2], color3Array[3] };


        colorData = ByteBuffer
                .allocateDirect(tmpColor.length * BYTES_PER_FLOAT)

                .order(ByteOrder.nativeOrder())

                .asFloatBuffer();

        colorData.put(tmpColor);
    }

    public FloatBuffer getColor()
    {
        return colorData;
    }

    public void Draw( Renderer renderer )
    {

        renderer.Draw(vertexData, color, 3, 0, true);
        renderer.Draw(vertexData, 1, 3, 0, false);

    }


    public void DrawWithColor( Renderer renderer )
    {

        renderer.Draw(vertexData, colorData, 3, 0, true);
        renderer.Draw(vertexData, colorData, 3, 0, false);

    }

    public void setEFT( int[] dofs )
    {
        EFT = dofs;
    }

    public void setDisp( int i, float disp ) { StaticDisp[i] =  disp; }

    public float getDisp( int i )
    {
        return StaticDisp[i];
    }

    public int getDof( int i )
    {
        return EFT[i];
    }

    public  void setStrain(float[] _strains) {strain = _strains; }

    public void setVonMisesStrain( float _strain) { vonMisesStrain = _strain; }

    public float getVonMisesStrain( ) { return vonMisesStrain; }

    public void setPos( int i )
    {
        pos = i;
    }

    public int getPos( )
    {
        return pos;
    }

    public float getStress( int i ) { return stress[i]; }

    public float getStrain( int i ) { return strain[i]; }

    public void setStress(float[] _stress) {stress = _stress; }

    public void setStress(int i, float _stress) { stress[i] = _stress; }

    public void setVonMises(float _vonMises) {vonMises = _vonMises; }

    public void setVonMisesStressAveragedToNodes( int i,float _stress ) {
        vonMisesStressAveragedToNodes[i] = _stress;
    }

    public float getVonMidesStressAveragedToNodes( int i ) { return vonMisesStressAveragedToNodes[i]; }

    public void setVonMisesStrainAveragedToNodes( int i,float _strain ) {
        vonMisesStrainAveragedToNodes[i] = _strain;
    }

    public float getVonMidesStrainAveragedToNodes( int i ) { return vonMisesStrainAveragedToNodes[i]; }

    public float[] getStrain(){return strain;}

    public float[] getStress(){return stress;}

    public float getVonMises(){return vonMises;}

}
