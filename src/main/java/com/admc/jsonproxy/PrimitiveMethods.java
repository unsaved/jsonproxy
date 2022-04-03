package com.admc.jsonproxy;

import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;

class PrimitiveMethods {
    /**
     * From https://stackoverflow.com/questions/25149412/how-to-convert-listt-to-array-t-for-primitive-types-using-generic-method
     */
    public static <P> P toPrimitiveArray(List<?> list, Class<P> arrayType) {
        if (!arrayType.isArray()) {
            throw new IllegalArgumentException(arrayType.toString());
        }
        Class<?> primitiveType = arrayType.getComponentType();
        if (!primitiveType.isPrimitive()) {
            throw new IllegalArgumentException(primitiveType.toString());
        }

        P array = arrayType.cast(Array.newInstance(primitiveType, list.size()));

        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, list.get(i));
        }

        return array;
    }

    /**
     * Purpose is not to return array length, which is trivial. It's to prove
     * that the passed value is indeed a real primitive integer array.
     *
     * @returns array length
     */
    public static int intArrayLen(int[] ia) {
        return ia.length;
    }

    /**
     * Purpose is more to prove that the passed value is indeed a real
     * primitive integer array array.
     *
     * @returns List<Integer> of member array lengths.  Member value will be
     *          null if the corresponding int[] is null.
     */
    public static List<Integer> intArrayArrayLens(int[][] iaa) {
        List<Integer> lengths = new ArrayList<>();
        for (int i = 0; i < iaa.length; i++)
            lengths.add(iaa[i] == null ? null : iaa[i].length);
        return lengths;
    }
}
