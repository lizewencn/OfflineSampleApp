package com.example.offline.comments;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.example.offline.model.Comment;
import com.example.offline.rx.SchedulersFacade;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

class CommentsViewModel extends ViewModel {

    private final AddCommentUseCase addCommentUseCase;

    private final SyncCommentUseCase syncCommentUseCase;

    private final GetCommentsUseCase getCommentsUseCase;

    private final SchedulersFacade schedulersFacade;

    private final CompositeDisposable disposables = new CompositeDisposable();

    private MutableLiveData<List<Comment>> commentsLiveData = new MutableLiveData<>();

    CommentsViewModel(AddCommentUseCase addCommentUseCase,
                      SyncCommentUseCase syncCommentUseCase,
                      GetCommentsUseCase getCommentsUseCase,
                      SchedulersFacade schedulersFacade) {
        this.addCommentUseCase = addCommentUseCase;
        this.syncCommentUseCase = syncCommentUseCase;
        this.getCommentsUseCase = getCommentsUseCase;
        this.schedulersFacade = schedulersFacade;

        queryComments();
    }

    @Override
    protected void onCleared() {
        disposables.clear();
    }

    void addComment(String commentText) {
        Timber.d("adding comment " + commentText);
        disposables.add(addCommentUseCase.addComment(commentText)
                .subscribeOn(schedulersFacade.io())
                .observeOn(schedulersFacade.ui())
                .subscribe(comment -> onAddCommentSuccess(comment),
                        t -> Timber.e(t, "add comment error")));
    }

    LiveData<List<Comment>> getLiveComments() {
        Timber.d("getting comments");
        return commentsLiveData;
    }

    void queryComments() {
        disposables.add(getCommentsUseCase.getComments()
                .subscribeOn(schedulersFacade.io())
                .observeOn(schedulersFacade.ui())
                .subscribe(commentList -> onGetCommentsSuccess(commentList),
                        t -> Timber.e(t, "get comments error")));
    }

    private void onGetCommentsSuccess(List<Comment> commentList) {
        Timber.d("get comments success");
        commentsLiveData.setValue(commentList);
    }

    private void onAddCommentSuccess(Comment comment) {
        Timber.d("add comment success");

        // refresh model changes in UI
        queryComments();

        // send sync comment request
        disposables.add(syncCommentUseCase.syncComment(comment)
                .subscribeOn(schedulersFacade.io())
                .observeOn(schedulersFacade.ui())
                .subscribe(() -> Timber.d("sync comment request sent success"),
                        t -> Timber.e(t, "sync comment request sent error")));
    }
}
