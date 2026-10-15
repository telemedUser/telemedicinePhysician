package com.telemedicine.common.core.result;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A success-or-failure value for code paths where throwing is the wrong tool.
 *
 * <p>External adapters are the motivating case: a provider returning "not eligible" is an ordinary
 * outcome that the caller must branch on, and modelling it as an exception makes the happy path
 * harder to read and the failure path easy to swallow.
 *
 * @param <T> success value
 * @param <E> failure value
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    /**
     * The components are named {@code payload} and {@code cause} rather than {@code value} and
     * {@code error} so the generated record accessors do not clash with the {@link Optional}-typed
     * methods below, which are what callers are expected to use.
     */
    record Success<T, E>(T payload) implements Result<T, E> {}

    record Failure<T, E>(E cause) implements Result<T, E> {}

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    default boolean isSuccess() {
        return this instanceof Success<T, E>;
    }

    default boolean isFailure() {
        return this instanceof Failure<T, E>;
    }

    default Optional<T> value() {
        return this instanceof Success<T, E> success
                ? Optional.ofNullable(success.payload())
                : Optional.empty();
    }

    default Optional<E> error() {
        return this instanceof Failure<T, E> failure
                ? Optional.ofNullable(failure.cause())
                : Optional.empty();
    }

    default <R> Result<R, E> map(Function<? super T, ? extends R> mapper) {
        return this instanceof Success<T, E> success
                ? Result.success(mapper.apply(success.payload()))
                : Result.failure(((Failure<T, E>) this).cause());
    }

    default <R> Result<R, E> flatMap(Function<? super T, Result<R, E>> mapper) {
        return this instanceof Success<T, E> success
                ? mapper.apply(success.payload())
                : Result.failure(((Failure<T, E>) this).cause());
    }

    default <F> Result<T, F> mapError(Function<? super E, ? extends F> mapper) {
        return this instanceof Failure<T, E> failure
                ? Result.failure(mapper.apply(failure.cause()))
                : Result.success(((Success<T, E>) this).payload());
    }

    default T orElse(T fallback) {
        return this instanceof Success<T, E> success ? success.payload() : fallback;
    }

    default T orElseGet(Supplier<? extends T> fallback) {
        return this instanceof Success<T, E> success ? success.payload() : fallback.get();
    }

    /** Unwraps the success value, or throws whatever {@code mapper} builds from the failure. */
    default <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> mapper) throws X {
        if (this instanceof Success<T, E> success) {
            return success.payload();
        }
        throw mapper.apply(((Failure<T, E>) this).cause());
    }

    default T orElseThrow() {
        if (this instanceof Success<T, E> success) {
            return success.payload();
        }
        throw new NoSuchElementException("Result is a failure: " + ((Failure<T, E>) this).cause());
    }
}
