package com.waylonbrown.lifecycleawarerx;

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

import com.waylonbrown.lifecycleawarerx.reactivetypes.BaseReactiveTypeWithObserver;

import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.MaybeTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;

/**
 * Transformer that is used by the compose() method of your stream to cache emitted items and subscribe to them once 
 * the {@link LifecycleOwner} is active.
 * 
 * @param <T> stream inner type (what you want returned in your subscription)
 * @param <R> reactive type
 * @param <O> observer type
 */
class LifecycleTransformer<T, R, O> implements ObservableTransformer<T, T>,
		SingleTransformer<T, T>,
		MaybeTransformer<T, T> {

	@NonNull
	private final BaseReactiveTypeWithObserver<R, O> baseReactiveType;
	@NonNull
	private final SubscribeWhenReadyObserver<R, O> lifecycleObserver;
	@NonNull
	private final LifecyclePredicate<T> lifecyclePredicate;

	LifecycleTransformer(@NonNull final LifecycleOwner lifecycleOwner,
						 @NonNull final BaseReactiveTypeWithObserver<R, O> baseReactiveType) {
		this.baseReactiveType = baseReactiveType;
		this.lifecycleObserver = new SubscribeWhenReadyObserver<>(lifecycleOwner);
		this.lifecyclePredicate = new LifecyclePredicate<>(lifecycleOwner);
	}

	@Override
	public ObservableSource<T> apply(final Observable<T> upstream) {
		Observable<T> transformedStream = upstream
				.cache() // Cache to replay emitted values to late subscriber
				.filter(lifecyclePredicate); // Filter to stop emitting items once LifecycleOwner is destroyed
		setReactiveType((R)transformedStream);
		
		return transformedStream;
	}

	/**
	 * NOTE: This throws a NoSuchElementException if the item is filtered out, so the onError
	 * is called after onDestroy() when using Single().
	 *
	 * @param upstream
	 * @return
	 */
	@Override
	public SingleSource<T> apply(Single<T> upstream) {
		Single<T> transformedStream = upstream
				.cache() // Cache to replay emitted values to late subscriber
				.filter(lifecyclePredicate) // Filter to stop emitting items once LifecycleOwner is destroyed
				.toSingle();
		setReactiveType((R)transformedStream);
		
		return transformedStream;
	}

	@Override
	public MaybeSource<T> apply(Maybe<T> upstream) {
		// Replay emitted values to late subscriber
		Maybe<T> transformedStream = upstream
				.cache() // Cache to replay emitted values to late subscriber
				.filter(lifecyclePredicate); // Filter to stop emitting items once LifecycleOwner is destroyed
		setReactiveType((R)transformedStream);
		
		return transformedStream;
	}

	private void setReactiveType(final R upstream) {
		baseReactiveType.setReactiveType(upstream);
		lifecycleObserver.setBaseReactiveType(baseReactiveType);
	}
}