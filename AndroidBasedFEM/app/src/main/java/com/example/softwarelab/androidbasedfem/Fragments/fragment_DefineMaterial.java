package com.example.softwarelab.androidbasedfem.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.example.softwarelab.androidbasedfem.R;
import com.example.softwarelab.androidbasedfem.Rendering.Rectangle;
import com.example.softwarelab.androidbasedfem.Rendering.Triangle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;


public class fragment_DefineMaterial extends android.support.v4.app.Fragment implements View.OnClickListener {

    private EditText editTextThickness;

    private List<String> pickerItemsNames     = new ArrayList<String>();
    private List<Float> pickerItemsYoungs    = new ArrayList<Float>();
    private List<Float> pickerItemsPoissonss = new ArrayList<Float>();
    private List<Float> pickerItemsDensities = new ArrayList<Float>();
    private NumberPicker numberPicker;

    private Float youngsModulus = 2e11f;
    private Float poissonsRatio = 0.3f;
    private Float thickness = 1.0f;
    private Float density = 1000f;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View fragmentDefineMaterialView = inflater.inflate(R.layout.layout_definematerial,container,false);

        editTextThickness = (EditText) fragmentDefineMaterialView.findViewById(R.id.editText_thickness);

        pickerItemsNames.add("Aluminum");
        pickerItemsYoungs.add(69e9f);
        pickerItemsPoissonss.add(0.33f);
        pickerItemsDensities.add(2712f);
        pickerItemsNames.add("Steel");
        pickerItemsYoungs.add(207e9f);
        pickerItemsPoissonss.add(0.3f);
        pickerItemsDensities.add(7850f);
        pickerItemsNames.add("Copper");
        pickerItemsYoungs.add(110e9f);
        pickerItemsPoissonss.add(0.34f);
        pickerItemsDensities.add(8940f);
        pickerItemsNames.add("ABS");
        pickerItemsYoungs.add(2.3e9f);
        pickerItemsPoissonss.add(0.35f);
        pickerItemsDensities.add(1.052f);
        pickerListCreate(fragmentDefineMaterialView);

        Button btnApproveMaterial = fragmentDefineMaterialView.findViewById(R.id.btnApproveMaterial);
        Button btnAddMaterial  = fragmentDefineMaterialView.findViewById(R.id.btnAddMaterial);
        btnApproveMaterial.setOnClickListener(this);
        btnAddMaterial.setOnClickListener(this);

        return fragmentDefineMaterialView;
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btnAddMaterial) {
            popupAddNewMaterial(v, pickerItemsNames);
        }

        if (v.getId() == R.id.btnApproveMaterial) {
            approveGeometries();
        }

    } // end of onClick


    // This function finalizes material definition and passes data to FEMPackage
    @SuppressLint("ClickableViewAccessibility")
    public boolean approveGeometries()
    {
        thickness = Float.valueOf(editTextThickness.getText().toString());
        saveMaterialToJSONFile(youngsModulus, poissonsRatio, thickness, density);
        LogToConsole("---------------------------------------------------------------------");
        LogToConsole("Material is defined to be used by FEM Package");
        LogToConsole("---------------------------------------------------------------------");
        return false;
    }

    public void pickerListCreate(View v){
        final String[] pickerItemArr = pickerItemsNames.toArray(new String[pickerItemsNames.size()]);

        numberPicker = v.findViewById(R.id.pickerMaterial);
        numberPicker.setDisplayedValues(null);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(pickerItemArr.length-1);
        numberPicker.setDisplayedValues(pickerItemArr);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                TextView tv = getActivity().findViewById(R.id.textViewMaterialData);
                youngsModulus = pickerItemsYoungs.get(i1);
                poissonsRatio = pickerItemsPoissonss.get(i1);
                density       = pickerItemsDensities.get(i1);
                tv.setText("\n    Young's Modulus:   "    +   youngsModulus + "\n" +
                           "    Poisson's Ratio:       " + poissonsRatio + "\n" +
                           "    Density:       "         + density + "\n");
            }
        });
    }

    public void popupAddNewMaterial(View v, final List<String> pickerItemsNames){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_addmaterial,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        final EditText editText_materialName     = (EditText) promptsView.findViewById(R.id.editText_popupAddMaterialName);
        final EditText editText_materialYoungs   = (EditText) promptsView.findViewById(R.id.editText_popupAddMaterialYoungs);
        final EditText editText_materialPoissons = (EditText) promptsView.findViewById(R.id.editText_popupAddMaterialPoissons);

        alertBuilder.setCancelable(true)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        pickerItemsNames.add(editText_materialName.getText().toString());
                        pickerItemsYoungs.add( Float.valueOf(editText_materialYoungs.getText().toString()) );
                        pickerItemsPoissonss.add( Float.valueOf(editText_materialPoissons.getText().toString()) );
                        pickerListCreate(getView());
                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
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

    public void saveMaterialToJSONFile(Float youngs, Float poissons, Float thickness, Float density){
        ///// Prepare String Data to be Written into the file
        try {
            JSONObject finalJSONObj = new JSONObject();    // final object to be written into the file

            JSONObject materialObj = new JSONObject();
            materialObj.put("YoungsModulus", youngs);
            materialObj.put("PoissonsRatio", poissons);
            materialObj.put("Thickness", thickness);
            materialObj.put("Density", density);

            finalJSONObj.put("material", materialObj);  // put the material object into final json object

            ///// Convert Final JSON Object into a String
            String jsonData = finalJSONObj.toString();

            ///// Write String to a File
            try
            {
                FileOutputStream fileOut;
                fileOut = getActivity().openFileOutput("materialToFEMPackage.json", Context.MODE_PRIVATE);
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

}
