package com.artemzin.androiddevelopmentculture.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.artemzin.androiddevelopmentculture.ADCApp;
import com.artemzin.androiddevelopmentculture.ApplicationModule;
import com.artemzin.androiddevelopmentculture.R;
import com.artemzin.androiddevelopmentculture.api.entities.Item;
import com.artemzin.androiddevelopmentculture.models.ItemsModel;
import com.artemzin.androiddevelopmentculture.ui.adapters.ItemsAdapter;
import com.artemzin.androiddevelopmentculture.ui.presenters.ItemsPresenter;
import com.artemzin.androiddevelopmentculture.ui.presenters.ItemsPresenterConfiguration;
import com.artemzin.androiddevelopmentculture.ui.views.ItemsView;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.Bind;
import butterknife.ButterKnife;
import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import rx.schedulers.Schedulers;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ItemsFragment extends BaseFragment implements ItemsView {

    @Subcomponent(modules = ItemsFragmentModule.class)
    public interface ItemsFragmentComponent {
        void inject(@NonNull ItemsFragment itemsFragment);
    }

    @Module
    public static class ItemsFragmentModule {

        @Provides
        @NonNull
        public ItemsPresenter provideItemsPresenter(@NonNull ItemsModel itemsModel) {
            return new ItemsPresenter(
                    ItemsPresenterConfiguration.builder().ioScheduler(Schedulers.io()).build(),
                    itemsModel
            );
        }
    }

    @Bind(R.id.items_loading_ui)
    View loadingUiView;

    @Bind(R.id.items_loading_error_ui)
    View errorUiView;

    @Bind(R.id.items_content_ui)
    RecyclerView contentUiRecyclerView;

    ItemsAdapter itemsAdapter;

    @Inject
    @Named(ApplicationModule.MAIN_THREAD_HANDLER)
    Handler mainThreadHandler;

    @Inject
    ItemsPresenter itemsPresenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ADCApp.get(getContext()).applicationComponent().plus(new ItemsFragmentModule()).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        contentUiRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), VERTICAL, false));
        itemsAdapter = new ItemsAdapter(getActivity().getLayoutInflater());
        contentUiRecyclerView.setAdapter(itemsAdapter);
        itemsPresenter.bindView(this);
        itemsPresenter.reloadData();
    }

    @SuppressWarnings("ResourceType")
    // Lint does not understand that we shift execution on Main Thread.
    @Override
    @WorkerThread
    public void showLoadingUi() {
        mainThreadHandler.post(() -> {
            loadingUiView.setVisibility(VISIBLE);
            errorUiView.setVisibility(GONE);
            contentUiRecyclerView.setVisibility(GONE);
        });
    }

    @SuppressWarnings("ResourceType")
    // Lint does not understand that we shift execution on Main Thread.
    @Override
    @WorkerThread
    public void showErrorUi(@NonNull Throwable error) {
        mainThreadHandler.post(() -> {
            loadingUiView.setVisibility(GONE);
            errorUiView.setVisibility(VISIBLE);
            contentUiRecyclerView.setVisibility(GONE);
        });
    }

    @SuppressWarnings("ResourceType")
    // Lint does not understand that we shift execution on Main Thread.
    @Override
    @WorkerThread
    public void showContentUi(@NonNull List<Item> items) {
        mainThreadHandler.post(() -> {
            loadingUiView.setVisibility(GONE);
            errorUiView.setVisibility(GONE);
            contentUiRecyclerView.setVisibility(VISIBLE);
            itemsAdapter.setData(items);
        });
    }

    @Override
    public void onDestroyView() {
        itemsPresenter.unbindView(this);
        super.onDestroyView();
    }
}
