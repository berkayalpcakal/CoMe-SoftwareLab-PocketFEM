package com.example.softwarelab.androidbasedfem.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.softwarelab.androidbasedfem.MainActivity;
import com.example.softwarelab.androidbasedfem.R;
import com.example.softwarelab.androidbasedfem.Rendering.Renderer;
import com.example.softwarelab.androidbasedfem.Rendering.Triangle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

public class fragment_Results extends android.support.v4.app.Fragment implements View.OnClickListener {

    private GLSurfaceView glSurfaceView;
    private Renderer renderer;
    private List<Triangle> TriangleElements = new ArrayList<Triangle>();
    private int numOfElements = 0;

    private String meshedGeometryJsonString;
    private String StaticDisplacementJsonString;
    private String materialJsonString;
    private String postProcessedDataString;


    private Float youngsModulus = 2e11f;
    private Float poissonsRatio = 0.3f;
    private Float thickness = 1.0f;
    private Float density = 2712f;
    private int numOfDof = 0;

    private SeekBar seekBar_scaling;

    TextView Coordinates;
    TextView minDisp;
    TextView maxDisp;
    TextView scalingFactor;

    Spinner spinner_results;

    boolean isPostProcessorCalled = false;
    ArrayList<float[]> ListStrain = new ArrayList<float[]>();
    ArrayList<float[]> ListStress = new ArrayList<float[]>();
    ArrayList<Float> ListvonMises = new ArrayList<Float>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentResultsView = inflater.inflate(R.layout.layout_reults, container, false);

        // Create OPenGLView and add to frame
        glSurfaceView = new GLSurfaceView(this.getActivity());
        glSurfaceView.setEGLContextClientVersion(2);

        RelativeLayout frame = fragmentResultsView.findViewById(R.id.layout_results);
        frame.addView(glSurfaceView, 0);

        renderer = new Renderer(this.getActivity());
        glSurfaceView.setRenderer(renderer);

        // Read and render geometry instantly
        clearGeometry();
        renderer.setAnimation(false);
        renderer.setShowColor(true);

        meshedGeometryJsonString = loadMeshedGeometryFromJsonFile("meshedGeometriesToFEMPackage.json");
        StaticDisplacementJsonString = loadStaticDisplacementFromJsonFile("StaticDisplacement.json");
        materialJsonString = loadMaterialFromJsonFile("materialToFEMPackage.json");

        renderer.setMaxMinValues();
        renderer.centerView();
        renderer.setFieldColor(0);

        renderer.addColorBartoRenderer( );

        // Some buttons
        Button btnCenterView = fragmentResultsView.findViewById(R.id.btnCenterView);
        btnCenterView.setOnClickListener(this);
        Button btnShowAnimation = fragmentResultsView.findViewById(R.id.btnShowAnimation);
        btnShowAnimation.setOnClickListener(this);
        Button btnStopAnimation = fragmentResultsView.findViewById(R.id.btnStopAnimation);
        btnStopAnimation.setOnClickListener(this);
        Button btnSelectEntity = fragmentResultsView.findViewById(R.id.btnSelectEntity);
        btnSelectEntity.setOnClickListener(this);

        // Spinner - select what to display on screen
        spinner_results = fragmentResultsView.findViewById(R.id.spinner_results);
        String[] items = new String[]{"u", "\u03C3", "\u03B5"}; // displacement, stress, strain
        ArrayAdapter<String> adapter_spinner = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, items);
        spinner_results.setAdapter(adapter_spinner);
        spinner_results.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == 0)
                {
                    renderer.setFieldColor(0);

                    String Text = "u_min: " + renderer.getMinDisplacement();
                    minDisp.setText(Text);

                    Text = "u_max: " + renderer.getMaxDisplacement();
                    maxDisp.setText(Text);
                }

                if (position == 1)
                {
                    if (!isPostProcessorCalled)
                    {
                        popupPostProcessing(view);
                        postProcessedDataString = ((MainActivity)getActivity()).postProcess(meshedGeometryJsonString, materialJsonString, StaticDisplacementJsonString);
                        loadPostProcessedDataFromJsonString(postProcessedDataString);
                        isPostProcessorCalled = true;
                        averageStressesAndStrain();
                    }
                    renderer.setMaxMinValues();
                    renderer.setFieldColor(1);

                    String Text = "s_min: " + renderer.getMinStress();
                    minDisp.setText(Text);

                    Text = "s_max: " + renderer.getMaxStress();
                    maxDisp.setText(Text);
                }

                if (position == 2)
                {
                    if (!isPostProcessorCalled) {
                        popupPostProcessing(view);
                        postProcessedDataString = ((MainActivity) getActivity()).postProcess(meshedGeometryJsonString, materialJsonString, StaticDisplacementJsonString);
                        loadPostProcessedDataFromJsonString(postProcessedDataString);
                        isPostProcessorCalled = true;
                        averageStressesAndStrain();
                    }
                    renderer.setMaxMinValues();
                    renderer.setFieldColor(2);

                    String Text = "epsilon_min: " + renderer.getMinStrain();
                    minDisp.setText(Text);

                    Text = "epsilon_max: " + renderer.getMaxStrain();
                    maxDisp.setText(Text);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });


        // Scaling displacement
        scalingFactor = fragmentResultsView.findViewById(R.id.scalingFactor);

        seekBar_scaling = fragmentResultsView.findViewById(R.id.seekBar_scaling);
        seekBar_scaling.setProgress(1);
        updateSeekBarScaling();
        updateScalingFactorText(1);
        seekBar_scaling.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                renderer.updateScalingFactor(progress);
                updateScalingFactorText(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // Write Coordinates to Screen, will be update when screen is touched
        Coordinates = fragmentResultsView.findViewById( R.id.Coordinates );
        String Text = "X: 0.00" + " Y: 0.00";
        Coordinates.setText( Text );
        Coordinates.setTextColor(Color.parseColor("#FFFFFF") );

        minDisp = fragmentResultsView.findViewById( R.id.minDisp );
        Text = "u_min: " + renderer.getMinDisplacement();
        minDisp.setText(Text);

        maxDisp = fragmentResultsView.findViewById( R.id.maxDisp );
        Text = "u_max: " + renderer.getMaxDisplacement();
        maxDisp.setText(Text);


        listenToTouch();
        return fragmentResultsView;

    }

    @Override
    public void onClick(View view) {

        if (view.getId()==R.id.btnCenterView)
        {
            renderer.centerView();
        }
        if (view.getId()==R.id.btnShowAnimation)
        {
            renderer.setAnimation(true);
        }
        if (view.getId()==R.id.btnStopAnimation)
        {
            renderer.setAnimation(false);
        }
        if (view.getId()==R.id.btnSelectEntity)
        {
            showNodalResults();
        }

    }

    /// This function Listens to all touch events
    // Handles scaling and panning
    // Updates screen coordinates after every touch event
    // Calls function to update coordinate system whenever screen size changes
    @SuppressLint("ClickableViewAccessibility")
    public void listenToTouch()
    {

        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {

            float[] Point;
            float[] movedPoint;
            float[] lastPoint;
            float xOffset = renderer.getXOffset();
            float yOffset = renderer.getYOffset();
            float viewScale = renderer.getViewScale();
            int a = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event ) {

                renderer.updateCoordinateSystem();
                renderer.updateColorBar();

                // If Pointer count is bigger than one, perform Scaling Action
                if (event.getPointerCount() > 1)
                {
                    a = 1;
                    renderer.mScaleDetector.onTouchEvent( event );
                    renderer.updateColorBar();

                } else // else perform panning action
                {
                    // This if statement makes sure, that we don't pan until the zooming action is finished
                    if (event.getAction() == MotionEvent.ACTION_UP && a == 1)
                    {
                        a = 0;
                    }
                    // Zooming Action will be performed if a == 0
                    if (event.getAction() == MotionEvent.ACTION_DOWN && a == 0) {
                        lastPoint = renderer.getNormalizedPointPosition(v, event, xOffset, yOffset, viewScale);
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE && lastPoint != null && a == 0) {

                        movedPoint = renderer.getNormalizedPointPosition(v, event, xOffset, yOffset, viewScale);

                        final float dx = ( lastPoint[0] - movedPoint[0] ) / viewScale + xOffset;// ) / viewScale;
                        final float dy = ( lastPoint[1] - movedPoint[1] ) / viewScale + yOffset;// ) / viewScale;

                        renderer.setViewOffset(dx, dy);
                        renderer.updateColorBar();
                    }
                }

                // Update view parameters, when touch is released
                if (event.getAction() == MotionEvent.ACTION_UP ) {
                    xOffset = renderer.getXOffset();
                    yOffset = renderer.getYOffset();
                    viewScale = renderer.getViewScale();
                }
                Point = renderer.getNormalizedPointPosition(v, event, xOffset, yOffset, viewScale);
                String Text = "X: " + Point[0] + " Y: " + Point[1];
                // This two calls update the Coordinates, which are visualized on the Screen
                Coordinates.setText(Text);

                return true;
            }

        }); // End of TouchListener
    } // End of Function listenToTouch


    @SuppressLint("ClickableViewAccessibility")
    public boolean showNodalResults()
    {
          glSurfaceView.setOnTouchListener(new View.OnTouchListener() {

            float[] point = {0.0f, 0.0f};
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                point = renderer.IntersectionTest( renderer.getNormalizedPointPosition(v, event) );

                int lengthTri = TriangleElements.size();
                float xDisp;
                float yDisp;
                float vonMises;
                float vonMisesStrain;
                float x;
                float y;

                for (int i = 0; i < lengthTri; i++) {

                    FloatBuffer Vertices = TriangleElements.get(i).getVertices();

                    for( int j = 0; j < 6; j = j + 2)
                    {
                        x = Vertices.get(j);
                        y = Vertices.get(j+1);

                        if( (point[0] == x) && (point[1] == y) ) {
                            xDisp = TriangleElements.get(i).getDisp(j);
                            yDisp = TriangleElements.get(i).getDisp(j + 1);
                            if( j < 2)
                            {
                                vonMises = TriangleElements.get(i).getVonMidesStressAveragedToNodes(0);
                                vonMisesStrain = TriangleElements.get(i).getVonMidesStrainAveragedToNodes(0);
                            }else if ( j < 4)
                            {
                                vonMises = TriangleElements.get(i).getVonMidesStressAveragedToNodes(1);
                                vonMisesStrain = TriangleElements.get(i).getVonMidesStrainAveragedToNodes(1);
                            } else
                            {
                                vonMises = TriangleElements.get(i).getVonMidesStressAveragedToNodes(2);
                                vonMisesStrain = TriangleElements.get(i).getVonMidesStrainAveragedToNodes(2);
                            }

                            popupShowNodalResult(v, xDisp, yDisp, vonMises, vonMisesStrain);
                            listenToTouch();
                            return true;
                        }
                    }
                }
                listenToTouch();
                return false;
            } // end of onTouch


        });// End of onTouchListener

        Log.e(TAG,"     End of onTouchListener ...");

        return false;
    } // End of showNodalResult()

    @SuppressLint("ClickableViewAccessibility")
    public boolean popupShowNodalResult(View v, float xDisp, float yDisp, float vonMises, float vonMisesStrain ){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_shownodalresult,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        final EditText text_xDisp  = (EditText) promptsView.findViewById(R.id.editText_xDisp);
        final EditText text_yDisp  = (EditText) promptsView.findViewById(R.id.editText_yDisp);
        final EditText text_vonMises  = (EditText) promptsView.findViewById(R.id.editText_vonMises);
        final EditText text_vonMisesStrain  = (EditText) promptsView.findViewById(R.id.editText_vonMisesStrain);

        String Text = "u_x: " + xDisp;
        text_xDisp.setText( Text );
        Text = "u_y: " + yDisp;
        text_yDisp.setText( Text );
        Text = "s: " + vonMises;
        text_vonMises.setText( Text );
        Text = "e: " + vonMisesStrain;
        text_vonMisesStrain.setText( Text );

        alertBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();

        return false;
    }


    public String loadStaticDisplacementFromJsonFile( String fileName ) {
        try {
            FileInputStream fileIn = getActivity().openFileInput(fileName);  // to read from device storage

            InputStreamReader inputStreamReader = new InputStreamReader(fileIn);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String lineData = bufferedReader.readLine();
            while (lineData != null) {
                stringBuffer.append(lineData);
                lineData = bufferedReader.readLine();
            }

            // Parse Data
            String strData = stringBuffer.toString();
            StaticDisplacementJsonString = strData;

            JSONObject obj = new JSONObject(strData);
            JSONArray DisplacemenentObj = obj.getJSONArray("Displacement");

            float[] tmpDispGlobal = new float[numOfDof];

            for (int i = 0; i < numOfDof; i++) {
                tmpDispGlobal[i] = ((float) DisplacemenentObj.getDouble(i));
            }

            int numTri = TriangleElements.size();
            float[] tmpDispTriangle = { 0, 0, 0 ,0 ,0 , 0 };

            for( int i = 0; i < numTri; i++ )
            {
                for( int j = 0; j < 6; j++ )
                {
                    TriangleElements.get(i).setDisp(j, ( tmpDispGlobal[ TriangleElements.get(i).getDof(j) - 1 ] ) );
                }
                //TriangleElements.get(i).setDisp( tmpDispTriangle );
            }

        } catch (Exception e) {
            Log.e("FileInputStream", e.getMessage());
            //LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
        return StaticDisplacementJsonString;

    }

    // This function reads the Json files and stores the information in a List of Triangles
    public String loadMeshedGeometryFromJsonFile(String fileName){
        try {
//            InputStream fileIn = getActivity().getAssets().open(fileName); // to read from assets folder of app
            FileInputStream fileIn = getActivity().openFileInput(fileName);  // to read from device storage

            InputStreamReader inputStreamReader = new InputStreamReader(fileIn);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String lineData = bufferedReader.readLine();
            while (lineData != null) {
                stringBuffer.append(lineData);
                lineData = bufferedReader.readLine();
            }

            // Parse Data
            String strData = stringBuffer.toString();
            meshedGeometryJsonString = strData;

            JSONObject obj = new JSONObject(strData);
            JSONObject meshedGeometryObj = obj.getJSONObject("meshedGeometry");

            numOfDof = meshedGeometryObj.getInt("numOfDof");
            JSONArray elementsArray = meshedGeometryObj.getJSONArray("elements");

            for (int i=0; i<elementsArray.length(); i++){
                JSONObject tempElement = elementsArray.getJSONObject(i);
                String _type = tempElement.getString("type");
                JSONArray eftarrobj = tempElement.getJSONArray("EFT");

                if (_type.equals("triangle")){
                    Triangle tempTriangle = new Triangle();
                    TriangleElements.add( tempTriangle );

                    JSONArray vertexDataX = new JSONArray(tempElement.getJSONArray("vertexDataX").toString());
                    JSONArray vertexDataY = new JSONArray(tempElement.getJSONArray("vertexDataY").toString());
                    float[] vertices = { (float) vertexDataX.getDouble(0), (float) vertexDataY.getDouble(0),
                            (float) vertexDataX.getDouble(1), (float) vertexDataY.getDouble(1),
                            (float) vertexDataX.getDouble(2), (float) vertexDataY.getDouble(2) };
                    tempTriangle.setVertexData( vertices );

                    int[] dofs = { (int) eftarrobj.getInt(0), (int) eftarrobj.getInt(1), (int) eftarrobj.getInt(2),
                            (int) eftarrobj.getInt(3), (int) eftarrobj.getInt(4), (int) eftarrobj.getInt(5) };

                    tempTriangle.setEFT( dofs );

                    renderer.addTriangleToRenderer(tempTriangle, false);
                    numOfElements += 1;
                }

            }
            renderer.centerView();

            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            //LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
        return meshedGeometryJsonString;
    }

    // This functions averages the VonMises Stress and Strain values on the correspondng nodes
    // And assigns it to each triangle -> Every triangle holds averaged stress and strain values of its nodes
    public void averageStressesAndStrain()
    {
        int sizeTri = TriangleElements.size();
        float sVon;
        float eVon;
        int division;

        List<Integer> accessTriangles = new ArrayList<Integer>();
        for( int i = 1; i <= numOfDof; i = i+2)
        {
            sVon = 0;
            eVon = 0;
            division = 0;
            accessTriangles.clear();
            for( int j = 0; j < sizeTri; j++)
            {
                if( TriangleElements.get(j).getDof(0) == i )
                {
                    sVon += TriangleElements.get(j).getVonMises();
                    eVon += TriangleElements.get(j).getVonMisesStrain();
                    TriangleElements.get(j).setPos(1);
                    accessTriangles.add(j);
                    division++;
                }
                if( TriangleElements.get(j).getDof(2) == i )
                {
                    sVon += TriangleElements.get(j).getVonMises();
                    eVon += TriangleElements.get(j).getVonMisesStrain();
                    TriangleElements.get(j).setPos(2);
                    accessTriangles.add(j);
                    division++;
                }
                if( TriangleElements.get(j).getDof(4) == i )
                {
                    sVon += TriangleElements.get(j).getVonMises();
                    eVon += TriangleElements.get(j).getVonMisesStrain();
                    TriangleElements.get(j).setPos(3);
                    accessTriangles.add(j);
                    division++;
                }
            }
            for( int j = 0; j < accessTriangles.size(); j++)
            {
                int pos = TriangleElements.get( accessTriangles.get(j) ).getPos();
                TriangleElements.get( accessTriangles.get(j) ).setVonMisesStressAveragedToNodes( pos - 1 , sVon/division);
                TriangleElements.get( accessTriangles.get(j) ).setVonMisesStrainAveragedToNodes( pos - 1 , eVon/division);
            }
        }
    }

    public void updateScalingFactorText(int scaling)
    {
        String Text = "x" + scaling;
        scalingFactor.setText(Text);
    }

    public void updateSeekBarScaling()
    {
        // Set maximum Scaling to average of minimum and maximum dimension of structure
        float ScalingSeekBar = (( renderer.getMinDim() + renderer.getMaxDim() ) / 2 ) /renderer.getMaxDisplacement();
        seekBar_scaling.setMax((int)ScalingSeekBar);
    }


    public String loadMaterialFromJsonFile(String fileName){
        String returnString = "";
        try {
//            InputStream fileIn = getActivity().getAssets().open(fileName); // to read from assets folder of app
            FileInputStream fileIn = getActivity().openFileInput(fileName);  // to read from device storage

            InputStreamReader inputStreamReader = new InputStreamReader(fileIn);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String lineData = bufferedReader.readLine();
            while (lineData != null) {
                stringBuffer.append(lineData);
                lineData = bufferedReader.readLine();
            }

            // Parse Data
            String strData = stringBuffer.toString();
            returnString = strData;
            JSONObject obj = new JSONObject(strData);
            String materialData = obj.getJSONObject("material").toString();
            JSONObject materialObj = new JSONObject(materialData);
            youngsModulus = Float.parseFloat( materialObj.getString("YoungsModulus") );
            poissonsRatio = Float.parseFloat( materialObj.getString("PoissonsRatio"));
            thickness     = Float.parseFloat( materialObj.getString("Thickness"));
            density       = Float.parseFloat( materialObj.getString("Density"));

            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            //LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
        return returnString;
    }

    public void loadPostProcessedDataFromJsonString(String strData){
        try {
            // Parse Data
            JSONObject obj = new JSONObject(strData);
            JSONArray dataArrayObj = obj.getJSONArray("PostProcessedData");

            for (int i=0; i<dataArrayObj.length(); i++)
            {
                JSONArray tempElementData = dataArrayObj.getJSONArray(i);

                //ListStrain.add(new float[]{(float)tempElementData.getDouble(0),(float)tempElementData.getDouble(1), (float)tempElementData.getDouble(2)});
                //ListStress.add(new float[]{(float)tempElementData.getDouble(3),(float)tempElementData.getDouble(4), (float)tempElementData.getDouble(5)});
                ListvonMises.add((float)tempElementData.getDouble(6));

                TriangleElements.get(i).setStrain(new float[]{(float)tempElementData.getDouble(0),(float)tempElementData.getDouble(1), (float)tempElementData.getDouble(2)});
                TriangleElements.get(i).setStress(new float[]{(float)tempElementData.getDouble(3),(float)tempElementData.getDouble(4), (float)tempElementData.getDouble(5)});
                TriangleElements.get(i).setVonMises((float)tempElementData.getDouble(6));
                TriangleElements.get(i).setVonMisesStrain((float)tempElementData.getDouble(7));
            }

            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            //LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
    }


    public void LogToConsole(String str){
        NavigationView navViewConsole    = this.getActivity().findViewById(R.id.nav_view_console);
        View headerViewConsole = navViewConsole.inflateHeaderView(R.layout.header_rightnavigationmenu);
        LinearLayout consoleLayout     = (LinearLayout) headerViewConsole.findViewById(R.id.layout_console);

        TextView tv = new TextView(this.getActivity());
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.LEFT;
        params.setMargins(10, 10, 10, 10);
        tv.setLayoutParams(params);
        tv.setText(str);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setBackgroundColor( 0x300a24);
        consoleLayout.addView(tv);
    }


    @SuppressLint("ClickableViewAccessibility")
    public boolean clearGeometry()
    {
        TriangleElements.clear();
        numOfElements = 0;
        renderer.clearFromRenderer();
        return  false;
    }

    public void popupPostProcessing(View v){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_solver_status,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        final TextView textView_solverStatus = (TextView) promptsView.findViewById(R.id.textView_solver_status);
        textView_solverStatus.setText("Post processor is running!");
        textView_solverStatus.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.holo_green_dark));

        alertBuilder.setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

} // End of class fragment


