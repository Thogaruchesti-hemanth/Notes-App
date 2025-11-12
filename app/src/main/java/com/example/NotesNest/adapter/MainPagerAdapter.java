package com.example.NotesNest.adapter;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.NotesNest.fragments.NotesFragment;
import com.example.NotesNest.fragments.RemindersFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new NotesFragment();
            case 1:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return new RemindersFragment();
                }
            default:
                return new NotesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // total fragments
    }
}
