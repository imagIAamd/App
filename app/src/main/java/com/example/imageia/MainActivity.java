package com.example.imageia;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.example.imageia.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private static final int HISTORIAL_ID = R.id.historial;
    private static final int PROFILE_ID = R.id.profile;
    String usuario = "";
    String correo = "";
    String telefono = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new InicioFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {

            switch (item.getItemId()) {
                case R.id.historial:
                    replaceFragment(new HistoryFragment());
                    break;
                case R.id.profile:
                    replaceFragment(new ProfileFragment());
                    break;
                case R.id.ullada:
                    replaceFragment(new UlladaFragment());
                    break;
            }
            return true;
        });


    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fragment instanceof ProfileFragment) {
            // Verificar si el fragmento ya tiene argumentos
            if (fragment.getArguments() == null) {
                // Si no tiene argumentos, crear una nueva instancia con los valores actuales
                ProfileFragment profileFragment = ProfileFragment.newInstance();
                fragmentTransaction.replace(R.id.frame_layout, profileFragment);
            } else {
                // Si ya tiene argumentos, simplemente reemplazar el fragmento existente
                fragmentTransaction.replace(R.id.frame_layout, fragment);
            }
        } else {
            fragmentTransaction.replace(R.id.frame_layout, fragment);
        }
        fragmentTransaction.commit();
    }

}
