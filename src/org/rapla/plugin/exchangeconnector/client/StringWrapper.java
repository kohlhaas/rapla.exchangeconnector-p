package org.rapla.plugin.exchangeconnector.client;

import java.util.Locale;

import org.rapla.entities.Named;

/**
* User: kuestermann
* Date: 16.09.12
* Time: 14:42
*/
class StringWrapper<T extends Named> {
    final T forObject;

    StringWrapper(T forObject) {
        this.forObject = forObject;
    }

    @Override
    public String toString() {
        return (forObject == null) ? super.toString() : forObject.getName(Locale.getDefault());
    }

    @Override
    public int hashCode() {
        return (forObject == null) ? super.hashCode() : forObject.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (forObject == null) ? super.equals(obj) :
                obj instanceof StringWrapper ? forObject.equals(((StringWrapper) obj).forObject) : forObject.equals(obj) ;
    }
}
