package com.example.NotesNest.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.NotesNest.fragments.AllNotesFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new AllNotesFragment();
//            case 1: return new ImportantFragment();
//            case 2: return new ReminderFragment();
//            case 3: return new TodoFragment();
//            case 4: return new WishesFragment();
            default: return new AllNotesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5; // total fragments
    }
}
