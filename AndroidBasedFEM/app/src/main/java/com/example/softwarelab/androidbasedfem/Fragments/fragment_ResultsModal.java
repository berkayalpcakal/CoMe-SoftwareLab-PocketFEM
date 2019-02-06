package com.example.softwarelab.androidbasedfem.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.softwarelab.androidbasedfem.R;
import com.example.softwarelab.androidbasedfem.Rendering.Renderer;
import com.example.softwarelab.androidbasedfem.Rendering.Triangle;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class fragment_ResultsModal extends android.support.v4.app.Fragment implements View.OnClickListener {

    private GLSurfaceView glSurfaceView;
    private Renderer renderer;
    private List<Triangle> TriangleElements = new ArrayList<Triangle>();
    private int numOfElements = 0;

    private String meshedGeometryJsonString;
    private String ModalAnalysisResultsJsonString;
    private int numOfDof = 0;
    private int numOfExcitationFrequencies = 0;
    private int numOfEigenvalues = 0;

    private SeekBar seekBar_scaling;

    private float[][] Displacement;
    private float[] excitationFrequencies;
    private int excitationFreqArrayPos = 0;
    private int DofOfInterest = 0;

    TextView Coordinates;
    TextView minDisp;
    TextView maxDisp;
    TextView excitationFrequency;
    TextView scalingFactor;

    GraphView graph;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentResultsView = inflater.inflate(R.layout.layout_resultsmodal, container, false);

        // Create OPenGLView and add to frame
        glSurfaceView = new GLSurfaceView(this.getActivity());
        glSurfaceView.setEGLContextClientVersion(2);

        RelativeLayout frame = fragmentResultsView.findViewById(R.id.layout_resultsmodal);
        frame.addView(glSurfaceView, 0);

        renderer = new Renderer(this.getActivity());
        glSurfaceView.setRenderer(renderer);

        // Read and render geometry instantly
        clearGeometry();
        renderer.setAnimation(false);
        renderer.setShowDisp(true);
        //loadGeometryFromJsonFile("document.json");
        //loadGeometryFromJsonFile( "document.json", false);
        //loadGeometryFromJsonFile("displacements.json", true);
        excitationFrequency = fragmentResultsView.findViewById( R.id.excitationFrequency );

        meshedGeometryJsonString = loadMeshedGeometryFromJsonFile("meshedGeometriesToFEMPackage.json");
        ModalAnalysisResultsJsonString = loadModalAnalysisResultsFromJsonFile("ModalAnalysisResults.json");
        renderer.setMaxMinValues();
        renderer.centerView();
        renderer.setFieldColor(0);

        renderer.addColorBartoRenderer();

        // Some buttons
        Button btnCenterView = fragmentResultsView.findViewById(R.id.btnCenterView);
        btnCenterView.setOnClickListener(this);

        Button btnSelectEntity = fragmentResultsView.findViewById(R.id.btnSelectEntity);
        btnSelectEntity.setOnClickListener(this);

        Button btnFrequencyUp = fragmentResultsView.findViewById(R.id.btnFrequencyUp);
        btnFrequencyUp.setOnClickListener(this);

        Button btnFrequencyDown = fragmentResultsView.findViewById(R.id.btnFrequencyDown);
        btnFrequencyDown.setOnClickListener(this);


        // Scaling displacement
        scalingFactor = fragmentResultsView.findViewById(R.id.scalingFactor);
        seekBar_scaling = fragmentResultsView.findViewById(R.id.seekBar_scaling);
        seekBar_scaling.setProgress(1);
//        seekBar_scaling.setMin((int)1);
        updateSeekBarScaling();
        updateScalingFactorText(1);
        //seekBar_scaling.setMax((int)ScalingSeekBar);
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

        if (view.getId()==R.id.btnSelectEntity)
        {
            showNodalResults();
        }

        if (view.getId()==R.id.btnFrequencyUp)
        {
            if( excitationFreqArrayPos < numOfExcitationFrequencies - 1 )
            {
                excitationFreqArrayPos++;
            }
            updateTriangles(excitationFreqArrayPos);
            renderer.setMaxMinValues();
            updateSeekBarScaling();
            String Text = "u_max: " + renderer.getMaxDisplacement();
            maxDisp.setText(Text);
            Text = "u_min: " + renderer.getMinDisplacement();
            minDisp.setText(Text);
            updateFrequencyText(excitationFreqArrayPos);
        }

        if (view.getId()==R.id.btnFrequencyDown)
        {
            if( excitationFreqArrayPos > 0 )
            {
                excitationFreqArrayPos--;
            }
            updateTriangles(excitationFreqArrayPos);
            renderer.setMaxMinValues();
            updateSeekBarScaling();
            String Text = "u_max: " + renderer.getMaxDisplacement();
            maxDisp.setText(Text);
            Text = "u_min: " + renderer.getMinDisplacement();
            minDisp.setText(Text);
            updateFrequencyText(excitationFreqArrayPos);

        }

        if (view.getId()==R.id.btnFrequencyResponseX)
        {
            popupShowFrequencyResponse(view, 0);
        }

        if (view.getId()==R.id.btnFrequencyResponseY)
        {
            popupShowFrequencyResponse(view, 1);
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

    public void loadGeometryFromJsonFile(String fileName, boolean deformed ){
        try {
            InputStream fileIn = getActivity().getAssets().open(fileName); // to read from assets folder of app
            //FileInputStream fileIn = getActivity().openFileInput(fileName);  // to read from device storage

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
            JSONObject obj = new JSONObject(strData);
            String geometriesData = obj.getJSONArray("rawGeometries").toString();

            JSONArray geometries = new JSONArray(geometriesData);
            for (int i=0; i<geometries.length(); i++){
                JSONObject tempGeometry = geometries.getJSONObject(i);
                String _type = tempGeometry.getString("type");

                if (_type.equals("triangle")){
                    Triangle tempTriangle = new Triangle();
                    TriangleElements.add( tempTriangle );

                    JSONArray vertexDataX = new JSONArray(tempGeometry.getJSONArray("vertexDataX").toString());
                    JSONArray vertexDataY = new JSONArray(tempGeometry.getJSONArray("vertexDataY").toString());
                    float[] vertices = { (float) vertexDataX.getDouble(0), (float) vertexDataY.getDouble(0),
                            (float) vertexDataX.getDouble(1), (float) vertexDataY.getDouble(1),
                            (float) vertexDataX.getDouble(2), (float) vertexDataY.getDouble(2) };
                    tempTriangle.setVertexData( vertices );

                    renderer.addTriangleToRenderer(tempTriangle, deformed);
                }

            }
            renderer.centerView();

            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            //LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
    }

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
                            DofOfInterest = j;
                            popupShowNodalResult(v, xDisp, yDisp);
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
    public boolean popupShowNodalResult(View v, float xDisp, float yDisp ){
        LayoutInflater li = LayoutInflater.from(getActivity());
        final View promptsView = li.inflate(R.layout.popup_shownodalresults_modal,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        final EditText text_xDisp  = (EditText) promptsView.findViewById(R.id.editText_xDisp);
        final EditText text_yDisp  = (EditText) promptsView.findViewById(R.id.editText_yDisp);
        String Text = "u_x: " + xDisp;
        text_xDisp.setText( Text );
        Text = "u_y: " + yDisp;
        text_yDisp.setText( Text );

        // Add Buttons
        Button btnFrequencyResponseX = promptsView.findViewById(R.id.btnFrequencyResponseX);
        btnFrequencyResponseX.setOnClickListener(this);

        Button btnFrequencyResponseY = promptsView.findViewById(R.id.btnFrequencyResponseY);
        btnFrequencyResponseY.setOnClickListener(this);


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


    @SuppressLint("ClickableViewAccessibility")
    public boolean popupShowFrequencyResponse(View v , int j ){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_showfrequencyresponse,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        // Add Graph
        graph = (GraphView) promptsView.findViewById(R.id.graph);


        DataPoint[] dp = new DataPoint[numOfExcitationFrequencies];
        for( int i = 0; i < numOfExcitationFrequencies; i++)
        {
            dp[i] = new DataPoint( excitationFrequencies[i] , 255/6* Math.log10( Math.abs( Displacement[DofOfInterest+j][i]) ) );
        }
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dp);


        series.setDrawDataPoints(true);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Hz");
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        //graph.getViewport().setMinX( excitationFrequencies[0] );
        graph.getViewport().setMaxX(excitationFrequencies[ numOfExcitationFrequencies - 1]);

        graph.getViewport().setMaxX(excitationFrequencies[ numOfExcitationFrequencies - 1]);


        graph.addSeries(series);


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


    public String loadModalAnalysisResultsFromJsonFile( String fileName ) {
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
            ModalAnalysisResultsJsonString = strData;

            JSONObject obj = new JSONObject(strData);

            numOfEigenvalues = obj.getInt("numEigenvalues");
            numOfExcitationFrequencies = obj.getInt("numFrequencies");

            JSONArray DisplacemenentObj = obj.getJSONArray("Displacement");

            JSONArray FrequencyObj = obj.getJSONArray("Frequencies");

            JSONArray EigenvalueObj = obj.getJSONArray( "Eigenfrequencies");

            Displacement = new float[numOfDof][numOfExcitationFrequencies];
            excitationFrequencies = new float[numOfExcitationFrequencies];

            for (int i = 0; i < numOfExcitationFrequencies; i++) {

                for (int j = 0; j < numOfDof; j++)
                {
                    Displacement[j][i] = ((float) DisplacemenentObj.getJSONArray(i).getDouble(j));
                }
                excitationFrequencies[i] = (float) FrequencyObj.getDouble(i);
            }

            LogToConsole( "Eigenfrequencies in Hz: "  );
            Float tmpString;

            if( obj.getBoolean("additionalEigenvalue")  )
            {
                tmpString = (float)EigenvalueObj.getDouble(numOfEigenvalues);
                LogToConsole( "( " + tmpString.toString() + " )" );
            }

            Integer counter = 0;
            for( int i = numOfEigenvalues; i >0; i--)
            {
                tmpString = (float)EigenvalueObj.getDouble(i-1);
                counter++;
                LogToConsole( (counter).toString()+ ": " + tmpString.toString()  );
            }

            updateTriangles( 0 );
            String Text = excitationFrequencies[0] + " Hz";
            excitationFrequency.setText(Text);

        } catch (Exception e) {
            Log.e("FileInputStream", e.getMessage());
            //LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
        return ModalAnalysisResultsJsonString;

    }

    public void updateTriangles( int freqIndex )
    {
        int numTri = TriangleElements.size();

        for( int i = 0; i < numTri; i++ )
        {
            for( int j = 0; j < 6; j++ )
            {
                TriangleElements.get(i).setDisp(j, ( Displacement[ (TriangleElements.get(i).getDof(j) - 1 )][ freqIndex ] ) );
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

    public void updateFrequencyText( int freqIndex )
    {
        String Text = excitationFrequencies[freqIndex] + " Hz";
        excitationFrequency.setText(Text);
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
}


