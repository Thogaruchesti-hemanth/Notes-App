package com.example.NotesNest.adapter;

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
        if (position == 1) {
            return new RemindersFragment();
        } else {
            return new NotesFragment();
        }

    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
