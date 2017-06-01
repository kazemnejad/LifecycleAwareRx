package com.waylonbrown.lifecycleawarerx;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;

import static org.junit.Assert.assertEquals;

public class LifecycleTest {

	private TestLifecycleOwner lifecycleOwner;
	private boolean methodOnViewCalled;
	private boolean onCompleteCalled;
	private boolean onErrorCalled;
	private int methodOnViewCalledCounter; // Only used with Observables
	
	@Before
	public void setup() {
		this.lifecycleOwner = new TestLifecycleOwner();
		this.methodOnViewCalled = false;
		this.onCompleteCalled = false;
		this.onErrorCalled = false;
		this.methodOnViewCalledCounter = 0;
	}
	
	/**
	 * Example of not using this library, where views are accessed after onDestroy() of your Activity or Fragment.
	 */
	@Test
	public void viewsAreCalledAfterDestroyWithoutLifecycleAwareRx() throws Exception {
		Observable.interval(1, TimeUnit.MILLISECONDS)
			.subscribeWith(new DisposableObserver<Long>() {
				@Override
				public void onNext(final Long value) {
					// This is what you don't want called after onDestroy() since your views won't exist.
					LifecycleTest.this.methodOnViewCalled = true;
				}

				@Override
				public void onError(final Throwable e) {
				}

				@Override
				public void onComplete() {
				}
			});

		lifecycleOwner.setState(Lifecycle.State.DESTROYED);
		methodOnViewCalled = false;	// Make sure there's a fresh state just as LifecycleOwner hits destroy

		// Need to wait to give it time to potentially fail
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(true, methodOnViewCalled);
	}

	@Test
	public void viewsAreCalledBeforeLifecycleIsReadyWithoutLifecycleAwareRx() throws Exception {
		// Lifecycle is "active" once it is STARTED, it's not ready yet at INITIALIZED or CREATED.
		lifecycleOwner.setState(Lifecycle.State.INITIALIZED);
		
		Observable.interval(1, TimeUnit.MILLISECONDS)
			.subscribeWith(new DisposableObserver<Long>() {
				@Override
				public void onNext(final Long value) {
					LifecycleTest.this.methodOnViewCalled = true;
				}
	
				@Override
				public void onError(final Throwable e) {
				}
	
				@Override
				public void onComplete() {
				}
			});

		// Need to wait to give it time to potentially fail
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(true, methodOnViewCalled);
	}

	/**
	 * Ending stream on LifecycleOwner's destroy.
	 */
	
	@Test
	public void viewsAreNotCalledWhenLifecycleDestroyedWithObservable() throws Exception {
		Observable.interval(1, TimeUnit.MILLISECONDS)
			.compose(LifecycleBinder.bind(lifecycleOwner, new DisposableObserver<Long>() {
				@Override
				public void onNext(final Long value) {
					LifecycleTest.this.methodOnViewCalled = true;
				}

				@Override
				public void onError(final Throwable e) {
					LifecycleTest.this.onErrorCalled = true;
				}

				@Override
				public void onComplete() {
					LifecycleTest.this.onCompleteCalled = true;
				}
			}));
		
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(true, methodOnViewCalled);

		lifecycleOwner.setState(Lifecycle.State.DESTROYED);
		methodOnViewCalled = false;	// Make sure there's a fresh state just as LifecycleOwner hits destroy
		onCompleteCalled = false;
		onErrorCalled = false;

		// Need to wait to give it time to potentially fail
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(false, methodOnViewCalled);
		assertEquals(false, onCompleteCalled);
		assertEquals(false, onErrorCalled);
	}

	@Test
	public void viewsAreNotCalledWhenLifecycleDestroyedWithSingle() throws Exception {
		Single.just("test")
			.compose(LifecycleBinder.bind(lifecycleOwner, new DisposableSingleObserver<String>() {
				@Override
				public void onSuccess(final String value) {
					LifecycleTest.this.methodOnViewCalled = true;
				}

				@Override
				public void onError(final Throwable e) {
					LifecycleTest.this.onErrorCalled = true;
				}
			}));

		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(true, methodOnViewCalled);

		lifecycleOwner.setState(Lifecycle.State.DESTROYED);
		methodOnViewCalled = false;	// Make sure there's a fresh state just as LifecycleOwner hits destroy
		onErrorCalled = false;

		// Need to wait to give it time to potentially fail
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(false, methodOnViewCalled);
		assertEquals(false, onErrorCalled);
	}

	// TODO: do these tests with compose called after destroy, not before
	@Test
	public void viewsAreNotCalledWhenLifecycleDestroyedWithMaybe() throws Exception {
		Maybe.just("test")
			.compose(LifecycleBinder.bind(lifecycleOwner, new DisposableMaybeObserver<String>() {
				@Override
				public void onSuccess(final String value) {
					LifecycleTest.this.methodOnViewCalled = true;
				}

				@Override
				public void onError(final Throwable e) {
					LifecycleTest.this.onErrorCalled = true;
				}

				@Override
				public void onComplete() {
					LifecycleTest.this.onCompleteCalled = true;
				}
			}));

		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(true, methodOnViewCalled);

		lifecycleOwner.setState(Lifecycle.State.DESTROYED);
		methodOnViewCalled = false;	// Make sure there's a fresh state just as LifecycleOwner hits destroy
		onCompleteCalled = false;
		onErrorCalled = false;

		// Need to wait to give it time to potentially fail
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(false, methodOnViewCalled);
		assertEquals(false, onCompleteCalled);
		assertEquals(false, onErrorCalled);
	}

	/**
	 * Delaying stream subscription until the LifecycleOwner is ready.
	 */

	@Test
	public void viewsAreOnlyCalledWhenLifecycleActiveWithObservable() throws Exception {
		// Lifecycle is "active" once it is STARTED, it's not ready yet at INITIALIZED or CREATED.
		lifecycleOwner.setState(Lifecycle.State.INITIALIZED);
		
		Observable.interval(1, TimeUnit.MILLISECONDS)
			.take(10)
			.compose(LifecycleBinder.bind(lifecycleOwner, new DisposableObserver() {
				@Override
				public void onNext(final Object value) {
					LifecycleTest.this.methodOnViewCalledCounter++;
				}
	
				@Override
				public void onError(final Throwable e) {
				}
	
				@Override
				public void onComplete() {
				}
			}));

		// Need to wait to give it time to potentially fail
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(0, methodOnViewCalledCounter);

		lifecycleOwner.setState(Lifecycle.State.CREATED);
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(0, methodOnViewCalledCounter);

		lifecycleOwner.setState(Lifecycle.State.STARTED);
		TimeUnit.MILLISECONDS.sleep(100);
		// At this point the views should now be called since the lifecycle is active
		assertEquals(10, methodOnViewCalledCounter);
	}

	@Test
	public void viewsAreOnlyCalledWhenLifecycleActiveWithSingle() throws Exception {
		// Lifecycle is "active" once it is STARTED, it's not ready yet at INITIALIZED or CREATED.
		lifecycleOwner.setState(Lifecycle.State.INITIALIZED);
		
		Single.just("test")
			.compose(LifecycleBinder.bind(lifecycleOwner, new DisposableSingleObserver<String>() {
				@Override
				public void onSuccess(final String value) {
					LifecycleTest.this.methodOnViewCalled = true;
				}
	
				@Override
				public void onError(final Throwable e) {
				}
			}));

		// Need to wait to give it time to potentially fail
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(false, methodOnViewCalled);

		lifecycleOwner.setState(Lifecycle.State.CREATED);
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(false, methodOnViewCalled);

		lifecycleOwner.setState(Lifecycle.State.STARTED);
		TimeUnit.MILLISECONDS.sleep(100);
		// At this point the views should now be called since the lifecycle is active
		assertEquals(true, methodOnViewCalled);
	}

	@Test
	public void viewsAreOnlyCalledWhenLifecycleActiveWithMaybe() throws Exception {
		// Lifecycle is "active" once it is STARTED, it's not ready yet at INITIALIZED or CREATED.
		lifecycleOwner.setState(Lifecycle.State.INITIALIZED);
		
		Maybe.just("test")
			.compose(LifecycleBinder.bind(lifecycleOwner, new DisposableMaybeObserver<String>() {
				@Override
				public void onSuccess(final String value) {
					LifecycleTest.this.methodOnViewCalled = true;
				}
	
				@Override
				public void onError(final Throwable e) {
				}
	
				@Override
				public void onComplete() {
				}
			}));

		// Need to wait to give it time to potentially fail
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(false, methodOnViewCalled);

		lifecycleOwner.setState(Lifecycle.State.CREATED);
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(false, methodOnViewCalled);

		lifecycleOwner.setState(Lifecycle.State.STARTED);
		TimeUnit.MILLISECONDS.sleep(100);
		// At this point the views should now be called since the lifecycle is active
		assertEquals(true, methodOnViewCalled);
	}

	@Test
	public void onlyLastItemEmittedOnceLifecycleActiveWithObservable() throws Exception {
		// Lifecycle is "active" once it is STARTED, it's not ready yet at INITIALIZED or CREATED.
		lifecycleOwner.setState(Lifecycle.State.INITIALIZED);

		final Long[] lastValue = {-1L};
		Observable.interval(1, TimeUnit.MILLISECONDS)
			.take(10)
			.takeLast(1)
			.compose(LifecycleBinder.bind(lifecycleOwner, new DisposableObserver<Long>() {
				@Override
				public void onNext(final Long value) {
					LifecycleTest.this.methodOnViewCalledCounter++;
					lastValue[0] = value;
				}
	
				@Override
				public void onError(final Throwable e) {
				}
	
				@Override
				public void onComplete() {
				}
			}));

		// Need to wait to give it time to potentially fail
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(0, methodOnViewCalledCounter);

		lifecycleOwner.setState(Lifecycle.State.CREATED);
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(0, methodOnViewCalledCounter);

		lifecycleOwner.setState(Lifecycle.State.STARTED);
		TimeUnit.MILLISECONDS.sleep(100);
		// At this point the views should now be called since the lifecycle is active
		assertEquals(1, methodOnViewCalledCounter);
		// Make sure it's the last item emitted, not the first
		assertEquals(9L, lastValue[0].longValue());
	}
	
	private static class TestLifecycle extends Lifecycle {
		
		private State state = State.STARTED;
		private LifecycleObserver observer;

		@Override
		public void addObserver(final LifecycleObserver observer) {
			this.observer = observer;
		}

		@Override
		public void removeObserver(final LifecycleObserver observer) {
		}

		@Override
		public State getCurrentState() {
			return state;
		}
		
		public void setCurrentState(State state) {
			this.state = state;
			if (observer != null) {
				// Used in bind()
				if (observer instanceof SubscribeWhenReadyObserver) {
					((SubscribeWhenReadyObserver)observer).onStateChange();
				} else if (observer instanceof LifecyclePredicate) {
					// Used in notDestroyed()
					((LifecyclePredicate)observer).onStateChange();
				}
			}
		}
	}
	
	private static class TestLifecycleOwner implements LifecycleOwner {

		private final TestLifecycle lifecycle;

		TestLifecycleOwner() {
			this.lifecycle = new TestLifecycle();
		}
		
		@Override
		public Lifecycle getLifecycle() {
			return lifecycle;
		}

		public void setState(final Lifecycle.State state) {
			lifecycle.setCurrentState(state);
		}
	}
}