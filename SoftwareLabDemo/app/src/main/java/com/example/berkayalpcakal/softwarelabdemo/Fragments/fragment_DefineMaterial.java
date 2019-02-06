package com.example.berkayalpcakal.softwarelabdemo.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
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

import com.example.berkayalpcakal.softwarelabdemo.R;

import java.util.ArrayList;
import java.util.List;


public class fragment_DefineMaterial extends android.support.v4.app.Fragment implements View.OnClickListener {

    List<String> pickerItemsNames     = new ArrayList<String>();
    List<Float> pickerItemsYoungs    = new ArrayList<Float>();
    List<Float> pickerItemsPoissonss = new ArrayList<Float>();
    NumberPicker numberPicker;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View fragmentDefineMaterialView = inflater.inflate(R.layout.layout_definematerial,container,false);

        pickerItemsNames.add("Aluminum");
        pickerItemsYoungs.add(69e9f);
        pickerItemsPoissonss.add(0.33f);
        pickerItemsNames.add("Steel");
        pickerItemsYoungs.add(207e9f);
        pickerItemsPoissonss.add(0.3f);
        pickerItemsNames.add("Copper");
        pickerItemsYoungs.add(110e9f);
        pickerItemsPoissonss.add(0.34f);
        pickerItemsNames.add("ABS");
        pickerItemsYoungs.add(2.3e9f);
        pickerItemsPoissonss.add(0.35f);
        pickerListCreate(fragmentDefineMaterialView);

        Button btnSaveMaterial = fragmentDefineMaterialView.findViewById(R.id.btnSaveMaterial);
        Button btnAddMaterial  = fragmentDefineMaterialView.findViewById(R.id.btnAddMaterial);
        btnSaveMaterial.setOnClickListener(this);
        btnAddMaterial.setOnClickListener(this);

        return fragmentDefineMaterialView;
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btnSaveMaterial) {
            LogToConsole("---------------------------> Material Saved <---------------------------");
            // Do something
        }

        if (v.getId() == R.id.btnAddMaterial) {
            popupAddNewMaterial(v, pickerItemsNames);
        }

    } // end of onClick


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
//                tv.setText("\n    Young's Modulus:   "    +   pickerItemArr[i1] + "\n" +
//                             "    Poisson's Ratio:       " + pickerItemArr[i1] + "\n");
                tv.setText("\n    Young's Modulus:   "    +   pickerItemsYoungs.get(i1) + "\n" +
                        "    Poisson's Ratio:       " + pickerItemsPoissonss.get(i1) + "\n");

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
        View           headerViewConsole = navViewConsole.inflateHeaderView(R.layout.header_rightnavigationmenu);
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

}
