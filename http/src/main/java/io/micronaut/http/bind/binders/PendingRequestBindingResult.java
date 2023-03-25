package io.micronaut.http.bind.binders;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.bind.ArgumentBinder;

@Experimental
public interface PendingRequestBindingResult<T> extends ArgumentBinder.BindingResult<T> {

    /**
     * @return True if the result is pending - not ready to be resolved
     */
    boolean isPending();

    /**
     * @return Was the binding requirement satisfied
     */
    default boolean isSatisfied() {
        return !isPending() && ArgumentBinder.BindingResult.super.isSatisfied();
    }

    /**
     * @return Is the value present and satisfied
     */
    default boolean isPresentAndSatisfied() {
        return !isPending() && ArgumentBinder.BindingResult.super.isPresentAndSatisfied();
    }


}
