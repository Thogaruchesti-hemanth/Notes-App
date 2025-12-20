package com.example.NotesNest.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LayoutToggleViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isGridLayout = new MutableLiveData<>(true); // default grid

    public LiveData<Boolean> getLayoutType() {
        return isGridLayout;
    }

    public void toggleLayout() {
        Boolean current = isGridLayout.getValue();
        isGridLayout.setValue(current == null || !current);
    }


    public void setLayout(boolean isGrid) {
        isGridLayout.setValue(isGrid);
    }

}
