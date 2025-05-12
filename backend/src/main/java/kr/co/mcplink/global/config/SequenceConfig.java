package kr.co.mcplink.global.config;

import kr.co.mcplink.global.annotation.AutoSequence;
import kr.co.mcplink.global.util.SequenceUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;

import java.lang.reflect.Field;

@Configuration
public class SequenceConfig {

    @Bean
    public BeforeConvertCallback<Object> sequenceBeforeConvertCallback(SequenceUtil sequenceUtil) {
        return (entity, collection) -> {
            Class<?> clazz = entity.getClass();
            if (clazz.isAnnotationPresent(AutoSequence.class)) {
                String coll = clazz.getAnnotation(AutoSequence.class).collection();
                try {
                    Field f = clazz.getDeclaredField("seq");
                    f.setAccessible(true);
                    Object curr = f.get(entity);
                    if (curr == null || ((Long) curr) <= 0) {
                        long next = sequenceUtil.generateSequence(coll);
                        f.set(entity, next);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return entity;
        };
    }
}