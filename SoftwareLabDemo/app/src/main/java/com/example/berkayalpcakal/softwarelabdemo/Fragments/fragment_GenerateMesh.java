package com.example.berkayalpcakal.softwarelabdemo.Fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.berkayalpcakal.softwarelabdemo.R;

public class fragment_GenerateMesh extends android.support.v4.app.Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){

        return inflater.inflate(R.layout.layout_generatemesh,container,false);
    }

}
