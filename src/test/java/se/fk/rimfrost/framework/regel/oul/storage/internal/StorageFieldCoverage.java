package se.fk.rimfrost.framework.regel.oul.storage.internal;

import static org.assertj.core.api.Assertions.assertThat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reflection helpers that make storage tests fail when a field is added to a
 * domain value type or a JPA entity without being covered by fixtures/mappers.
 */
final class StorageFieldCoverage
{
   private StorageFieldCoverage()
   {
   }

   /**
    * Walks every accessor of the Immutables value interface(s) implemented by
    * {@code value} (recursing into nested value types from the given packages)
    * and fails if any accessor returns null, an empty collection, or a
    * primitive default — unless its dotted path is in {@code excluded}.
    */
   static void assertFullyPopulated(Object value, Set<String> excluded, String... valuePackages)
   {
      List<String> missing = new ArrayList<>();
      walk(value, "", excluded, List.of(valuePackages), missing);
      assertThat(missing)
            .as("Fixture leaves these properties unset. Populate them (and map them in storage), "
                  + "or add them to the explicit exclusion list with a reason")
            .isEmpty();
   }

   private static void walk(Object value, String prefix, Set<String> excluded, List<String> pkgs, List<String> missing)
   {
      for (Method m : valueInterface(value, pkgs).getMethods())
      {
         if (m.getParameterCount() != 0 || Modifier.isStatic(m.getModifiers()) || m.isDefault())
         {
            continue;
         }
         String path = prefix + m.getName();
         if (excluded.contains(path))
         {
            continue;
         }
         Object v;
         try
         {
            v = m.invoke(value);
         }
         catch (ReflectiveOperationException e)
         {
            throw new IllegalStateException(e);
         }
         if (isUnset(v))
         {
            missing.add(path);
         }
         else if (isValueType(v, pkgs))
         {
            walk(v, path + ".", excluded, pkgs, missing);
         }
      }
   }

   /**
    * Fails if any persistent (non-static, non-transient) field of a JPA entity
    * still holds its Java default after the mapper ran. Technical columns
    * managed by JPA (version, created/updated timestamps) are passed in
    * {@code excluded}.
    */
   static void assertAllEntityFieldsSet(Object entity, Set<String> excluded)
   {
      List<String> missing = new ArrayList<>();
      for (Field f : persistentFields(entity.getClass()))
      {
         if (!excluded.contains(f.getName()) && isUnset(read(f, entity)))
         {
            missing.add(f.getName());
         }
      }
      assertThat(missing)
            .as("Entity fields not written by the mapper")
            .isEmpty();
   }

   static List<Field> persistentFields(Class<?> type)
   {
      List<Field> fields = new ArrayList<>();
      for (Field f : type.getDeclaredFields())
      {
         int mod = f.getModifiers();
         if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic()
               || f.isAnnotationPresent(jakarta.persistence.Transient.class)
               || f.getName().startsWith("$$_hibernate"))
         {
            continue;
         }
         f.setAccessible(true);
         fields.add(f);
      }
      return fields;
   }

   private static Object read(Field f, Object target)
   {
      try
      {
         return f.get(target);
      }
      catch (IllegalAccessException e)
      {
         throw new IllegalStateException(e);
      }
   }

   private static boolean isUnset(Object v)
   {
      if (v == null)
      {
         return true;
      }
      if (v instanceof Collection<?> c)
      {
         return c.isEmpty();
      }
      if (v instanceof Map<?, ?> m)
      {
         return m.isEmpty();
      }
      if (v instanceof Number n)
      {
         return n.doubleValue() == 0d;
      }
      if (v instanceof Boolean b)
      {
         return !b;
      }
      return false;
   }

   private static boolean isValueType(Object v, List<String> pkgs)
   {
      return pkgs.stream().anyMatch(p -> v.getClass().getPackageName().equals(p));
   }

   private static Class<?> valueInterface(Object value, List<String> pkgs)
   {
      for (Class<?> i : value.getClass().getInterfaces())
      {
         if (pkgs.contains(i.getPackageName()))
         {
            return i;
         }
      }
      throw new IllegalArgumentException("No value interface from " + pkgs + " on " + value.getClass());
   }
}
