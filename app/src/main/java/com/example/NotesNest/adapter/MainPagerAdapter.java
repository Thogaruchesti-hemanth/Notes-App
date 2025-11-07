package com.example.NotesNest.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.NotesNest.fragments.AllNotesFragment;
import com.example.NotesNest.fragments.RemindersFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new AllNotesFragment();
            case 2: return new RemindersFragment();
            default: return new AllNotesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5; // total fragments
    }
}
