package com.example.imageia;// ProfileFragment.java
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProfileFragment extends Fragment {

    private TextView textViewName, textViewEmail, textViewPhone;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        // Obtener referencias a los TextViews
        textViewName = rootView.findViewById(R.id.textViewName);
        textViewEmail = rootView.findViewById(R.id.textViewEmail);
        textViewPhone = rootView.findViewById(R.id.textViewPhone);

        // Leer los datos del archivo SVG y establecerlos en los TextViews
        readDataFromSVG();

        return rootView;
    }

    private void readDataFromSVG() {
        // Ruta del archivo SVG
        File file = new File(requireContext().getFilesDir(), "data.svg");

        // Verificar si el archivo existe
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);

                // Leer cada línea del archivo SVG
                String line;
                while ((line = br.readLine()) != null) {
                    // Procesar la línea para obtener los datos
                    if (line.contains("Correo:")) {
                        String correo = line.substring(line.indexOf(":") + 2);
                        textViewEmail.setText(correo);
                    } else if (line.contains("Usuario:")) {
                        String usuario = line.substring(line.indexOf(":") + 2);
                        textViewName.setText(usuario);
                    } else if (line.contains("Teléfono:")) {
                        String telefono = line.substring(line.indexOf(":") + 2);
                        textViewPhone.setText(telefono);
                    }
                }

                br.close();
                isr.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("ProfileFragment", "Error al leer el archivo SVG: " + e.getMessage());
            }
        } else {
            Log.e("ProfileFragment", "El archivo SVG no existe");
        }
    }
}
