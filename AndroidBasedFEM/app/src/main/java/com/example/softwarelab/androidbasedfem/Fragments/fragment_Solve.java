package com.example.softwarelab.androidbasedfem.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.softwarelab.androidbasedfem.MainActivity;
import com.example.softwarelab.androidbasedfem.R;
import com.example.softwarelab.androidbasedfem.Rendering.Renderer;
import com.example.softwarelab.androidbasedfem.Rendering.Triangle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class fragment_Solve extends android.support.v4.app.Fragment implements View.OnClickListener {

    private GLSurfaceView glSurfaceView;
    private Renderer renderer;
    private List<Triangle> TriangleElements = new ArrayList<Triangle>();

    // These are only default values and will be overwritten according to user input
    private Float youngsModulus = 2e11f;
    private Float poissonsRatio = 0.3f;
    private Float thickness = 1.0f;
    private Float density = 2712f;
    private int numOfDof = 0;
    private int numOfMeshElements = 0;

    private float f_min = 1;
    private float f_max = 100;
    private int f_no  = 10;
    private int numOfEigen = 10;
    private float damping1 = 0;
    private float damping2 = 0;

    private String modalSettingsString = "";

    private String meshedGeometryJsonString;
    private String materialJsonString;
    private String bcJsonString;
    private String dispString = "";

    private TextView textView_summary;
    private EditText editText_numOfEigen;
    private EditText editText_fmin;
    private EditText editText_fmax;
    private EditText editText_fno;

    private CheckBox checkBox_damping;
    private boolean  isDampingIncluded = false;
    private EditText editText_damping1;
    private EditText editText_damping2;
    private TextView textView_damping1;
    private TextView textView_damping2;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentSolveView = inflater.inflate(R.layout.layout_solve, container, false);

        // Create OPenGLView and add to frame
        glSurfaceView = new GLSurfaceView(this.getActivity());
        glSurfaceView.setEGLContextClientVersion(2);

        RelativeLayout frame = fragmentSolveView.findViewById(R.id.layout_solve);
        frame.addView(glSurfaceView, 0);

        renderer = new Renderer(this.getActivity());
        glSurfaceView.setRenderer(renderer);

        // Read meshed geometry and render
        clearGeometry();
        meshedGeometryJsonString = loadMeshedGeometryFromJsonFile("meshedGeometriesToFEMPackage.json");
        renderer.setMaxMinValues();
        renderer.centerView();

        // Read BC data and render
        bcJsonString = loadBCFromJsonFile("BCToFEMPackage.json");

        // Read material data
        materialJsonString = loadMaterialFromJsonFile("materialToFEMPackage.json");

        // Some buttons
        Button btnSolveFEM  = fragmentSolveView.findViewById(R.id.btnSolveFEM);
        btnSolveFEM.setOnClickListener(this);

        Button btnSolveModal  = fragmentSolveView.findViewById(R.id.btnSolveModal);
        btnSolveModal.setOnClickListener(this);

        // Summary Information
        textView_summary = fragmentSolveView.findViewById(R.id.textViewSummaryStaticSolve);
        textView_summary.setText(   "\nYoung's Modulus:   "    +   youngsModulus + "\n" +
                                    "Poisson's Ratio:       " + poissonsRatio + "\n" +
                                    "Density:       "         + density + "\n" +
                                    "Thickness:       "         + thickness + "\n" +
                                    "Num of Dofs: " + numOfDof + "\n" +
                                    "Num of Elements: " + numOfMeshElements + "\n");

        // Get info for modal analysis
        editText_numOfEigen = fragmentSolveView.findViewById(R.id.editText_numOfEigen);
        editText_fmin = fragmentSolveView.findViewById(R.id.editText_fmin);
        editText_fmax = fragmentSolveView.findViewById(R.id.editText_fmax);
        editText_fno = fragmentSolveView.findViewById(R.id.editText_numOfF);

        checkBox_damping = fragmentSolveView.findViewById(R.id.checkBox_damping);
        textView_damping1 = fragmentSolveView.findViewById(R.id.textView_damping1);
        textView_damping2 = fragmentSolveView.findViewById(R.id.textView_damping2);
        editText_damping1 = fragmentSolveView.findViewById(R.id.editText_damping1);
        editText_damping2 = fragmentSolveView.findViewById(R.id.editText_damping2);

        checkBox_damping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!((CheckBox) v).isChecked()) {
                    isDampingIncluded = false;

                    textView_damping1.setVisibility(TextView.GONE);
                    textView_damping2.setVisibility(TextView.GONE);

                    editText_damping1.setVisibility(EditText.GONE);
                    editText_damping2.setVisibility(EditText.GONE);
                }

                if (((CheckBox) v).isChecked()) {
                    isDampingIncluded = true;

                    textView_damping1.setVisibility(TextView.VISIBLE);
                    textView_damping2.setVisibility(TextView.VISIBLE);

                    editText_damping1.setVisibility(TextView.VISIBLE);
                    editText_damping2.setVisibility(TextView.VISIBLE);

                    damping1 = Float.valueOf(editText_damping1.getText().toString());
                    damping2 = Float.valueOf(editText_damping2.getText().toString());
                }
            }
        });

        return fragmentSolveView;
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.btnSolveFEM) {
//            Toast.makeText(getActivity(),"Solver is running...!",Toast.LENGTH_SHORT).show();
            dispString = ((MainActivity)getActivity()).solveStaticSystem(meshedGeometryJsonString, materialJsonString, bcJsonString);
            popupSolved(view);
            try
            {
                FileOutputStream fileOut;
                fileOut = getActivity().openFileOutput("StaticDisplacement.json", Context.MODE_PRIVATE);
                fileOut.write(dispString.getBytes());
                fileOut.close();
                LogToConsole("Static Displacements is saved to file: StaticDisplacement.json" );
            }
            catch (Exception e)
            {
                Log.e("FileOutputStream", e.getMessage());
                LogToConsole("Exception on FileOutputStream: Couldn't write to file..");
            }

            LogToConsole("---------------------------------------------------------------------");
            LogToConsole("PROBLEM IS SOLVED!! CHECK RESULTS!!!");
            LogToConsole("---------------------------------------------------------------------");
        }

        if (view.getId() == R.id.btnSolveModal){
            numOfEigen = Integer.valueOf(editText_numOfEigen.getText().toString());
            f_min = Float.valueOf(editText_fmin.getText().toString());
            f_max = Float.valueOf(editText_fmax.getText().toString());
            f_no = Integer.valueOf(editText_fno.getText().toString());

            if( isDampingIncluded )
            {
                damping1 = Float.valueOf( editText_damping1.getText().toString());
                damping2 = Float.valueOf( editText_damping2.getText().toString());
            }

            saveModalToJSONFile();

//            Toast.makeText(getActivity(),"Solver is running...!",Toast.LENGTH_SHORT).show();
            String dispModalString = ((MainActivity)getActivity()).solveDynamicSystem(meshedGeometryJsonString, materialJsonString, bcJsonString, modalSettingsString);
            popupSolved(view);

            try
            {
                FileOutputStream fileOut;
                fileOut = getActivity().openFileOutput("ModalAnalysisResults.json", Context.MODE_PRIVATE);
                fileOut.write(dispModalString.getBytes());
                fileOut.close();
                LogToConsole("Modal Analysis Results are saved to file: ModalAnalysisResults.json" );
            }
            catch (Exception e)
            {
                Log.e("FileOutputStream", e.getMessage());
                LogToConsole("Exception on FileOutputStream: Couldn't write to file..");
            }

            LogToConsole("---------------------------------------------------------------------");
            LogToConsole("PROBLEM IS SOLVED!! CHECK RESULTS!!!");
            LogToConsole("---------------------------------------------------------------------");
        }
    }

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

            numOfMeshElements = elementsArray.length();
            for (int i=0; i<elementsArray.length(); i++){
                JSONObject tempElement = elementsArray.getJSONObject(i);
                String _type = tempElement.getString("type");

                if (_type.equals("triangle")){
                    Triangle tempTriangle = new Triangle();
                    TriangleElements.add( tempTriangle );

                    JSONArray vertexDataX = new JSONArray(tempElement.getJSONArray("vertexDataX").toString());
                    JSONArray vertexDataY = new JSONArray(tempElement.getJSONArray("vertexDataY").toString());
                    float[] vertices = { (float) vertexDataX.getDouble(0), (float) vertexDataY.getDouble(0),
                            (float) vertexDataX.getDouble(1), (float) vertexDataY.getDouble(1),
                            (float) vertexDataX.getDouble(2), (float) vertexDataY.getDouble(2) };
                    tempTriangle.setVertexData( vertices );

                    renderer.addTriangleToRenderer(tempTriangle, false);
                }

            }

            renderer.centerView();

            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
        return meshedGeometryJsonString;
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
//            LogToConsole(youngsModulus.toString());
//            LogToConsole(poissonsRatio.toString());
//            LogToConsole(thickness.toString());
//            LogToConsole(density.toString());


            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            //LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
        return returnString;
    }

    public String loadBCFromJsonFile(String fileName){
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

            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            //LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
        return returnString;
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean clearGeometry()
    {
        TriangleElements.clear();
        renderer.clearFromRenderer();
        return  false;
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

    public void saveModalToJSONFile(){
        ///// Prepare String Data to be Written into the file
        try {
            JSONObject finalJSONObj = new JSONObject();    // final object to be written into the file

            JSONObject materialObj = new JSONObject();
            materialObj.put("NumOfEigen", numOfEigen);
            materialObj.put("Freq_min", f_min);
            materialObj.put("Freq_max", f_max);
            materialObj.put("Freq_no", f_no);
            materialObj.put("Damping1", damping1);
            materialObj.put("Damping2", damping2);
            materialObj.put("withDamping", isDampingIncluded);

            finalJSONObj.put("ModalSettings", materialObj);  // put the material object into final json object

            ///// Convert Final JSON Object into a String
            String jsonData = finalJSONObj.toString();
            modalSettingsString = jsonData;
            ///// Write String to a File
            try
            {
                FileOutputStream fileOut;
                fileOut = getActivity().openFileOutput("modalSettingsToFEMPackage.json", Context.MODE_PRIVATE);
                fileOut.write(jsonData.getBytes());
                fileOut.close();
            }
            catch (Exception e)
            {
                Log.e("FileOutputStream", e.getMessage());
                LogToConsole("Exception on FileOutputStream: Couldn't write to file..");
            }

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void popupSolved(View v){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_solver_status,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        final TextView textView_solverStatus = (TextView) promptsView.findViewById(R.id.textView_solver_status);
        textView_solverStatus.setText("Solved! Check Results!");
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

} // End of fragment class
