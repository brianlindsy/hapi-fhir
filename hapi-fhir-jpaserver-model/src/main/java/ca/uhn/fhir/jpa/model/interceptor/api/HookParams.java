package ca.uhn.fhir.jpa.model.interceptor.api;

/*-
 * #%L
 * HAPI FHIR Model
 * %%
 * Copyright (C) 2014 - 2019 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HookParams {

	private ListMultimap<Class<?>, Object> myParams = ArrayListMultimap.create();

	/**
	 * Constructor
	 */
	public HookParams() {
	}

	/**
	 * Constructor
	 */
	public HookParams(Object... theParams) {
		for (Object next : theParams) {
			add(next);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void add(T theNext) {
		Class<T> nextClass = (Class<T>) theNext.getClass();
		add(nextClass, theNext);
	}

	public <T> HookParams add(Class<T> theType, T theParam) {
		return doAdd(theType, theParam);
	}

	/**
	 * This is useful for providing a lazy-loaded (generally expensive to create)
	 * parameters
	 */
	public <T> HookParams addSupplier(Class<T> theType, Supplier<T> theParam) {
		return doAdd(theType, theParam);
	}

	private <T> HookParams doAdd(Class<T> theType, Object theParam) {
		Validate.isTrue(theType.equals(Supplier.class) == false, "Can not add parameters of type Supplier");
		myParams.put(theType, theParam);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> theParamType, int theIndex) {
		List<Object> objects = myParams.get(theParamType);
		Object retVal = null;
		if (objects.size() > theIndex) {
			retVal = objects.get(theIndex);
		}

		retVal = unwrapValue(retVal);

		return (T) retVal;
	}

	private Object unwrapValue(Object theValue) {
		if (theValue instanceof Supplier) {
			theValue = ((Supplier) theValue).get();
		}
		return theValue;
	}

	/**
	 * Returns an unmodifiable multimap of the params, where the
	 * key is the param type and the value is the actual instance
	 */
	public ListMultimap<Class<?>, Object> getParamsForType() {
		ArrayListMultimap<Class<?>, Object> retVal = ArrayListMultimap.create();
		myParams.entries().forEach(entry -> retVal.put(entry.getKey(), unwrapValue(entry.getValue())));
		return Multimaps.unmodifiableListMultimap(retVal);
	}

	public Collection<Object> values() {
		return
			Collections.unmodifiableCollection(myParams.values())
				.stream()
				.map(t -> unwrapValue(t))
				.collect(Collectors.toList());
	}
}
