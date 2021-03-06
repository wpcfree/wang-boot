package com.wpc.common.mongo.dao.shardcollection;

import com.wpc.common.mongo.bean.Entity;
import com.wpc.common.mongo.dao.multimongo.AbstractMultiMongoEntityDao;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mongodb.MongoCollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

/**
 * Created by 王鹏程 on 2018/11/15 0015.
 */
public abstract class AbstractShardEntityDao<T extends Entity> extends AbstractMultiMongoEntityDao<T> {
    /**
     * The maximum shard count, used if a higher value is implicitly specified.
     * MUST be a power of two <= 1<<10.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 10;
    /**
     * 实体类类型
     */
    private ParameterizedType parameterizedType = (ParameterizedType) this.getClass().getGenericSuperclass();
    /**
     * 实体类Class
     */
    private Class<T> classType = (Class<T>) parameterizedType.getActualTypeArguments()[0];
    /**
     * 集合名称
     */
    private String collectionName;
    /**
     * 分表Field
     */
    private Field shardingField;
    /**
     * 分表列名
     */
    private String shardingColumn;
    /**
     * 分表数
     */
    private int shardingCount;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AbstractShardEntityDao() {
        logger.info("start initializing shard dao for collection :{}", collectionName);
        String preferedCollectionName = MongoCollectionUtils.getPreferredCollectionName(classType);
        if (classType.isAnnotationPresent(Document.class)) {
            Document document = classType.getAnnotation(Document.class);
            collectionName = document.collection();
            collectionName = StringUtils.isEmpty(collectionName) ? preferedCollectionName : collectionName;
        }

        List<Field> fieldsWithShardingKey = FieldUtils.getFieldsListWithAnnotation(classType, ShardingKey.class);
        Assert.isTrue(fieldsWithShardingKey.size() == 1, "only one field could be annotated with ShardingKey annotation but find multi !");
        this.shardingField = fieldsWithShardingKey.get(0);
        ShardingKey shardingKey = AnnotationUtils.getAnnotation(shardingField, ShardingKey.class);
//        ShardingKey shardingKey = shardingField.getAnnotation(ShardingKey.class);
        assert shardingKey != null;
        this.shardingColumn = shardingKey.value();
        if (StringUtils.isEmpty(shardingColumn)) {
            throw new IllegalArgumentException("ShardingKey should have a specific shardingColumn !");
        }
        int shardingCount = shardingKey.shardingCount();
        this.shardingCount = shardingCountFor(shardingCount);
        logger.info("finished initializing shard dao for collection :{}", collectionName);
    }

    private int shardingCountFor(int i) {
        int pre = i;
        if (i < 0)
            throw new IllegalArgumentException("Illegal initial shard count : " + i);
        if (i > MAXIMUM_CAPACITY)
            i = MAXIMUM_CAPACITY;
        int result = tableSizeFor(i);
        if (pre != result) {
            logger.warn("Bad initial shard count : {}, converted to : {}", pre, result);
        }
        return result;
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    static final int tableSizeFor(int cap) {
        //减一的目的在于如果cap本身就是2的次幂，保证结果是原值，不减一的话，结果就成了cap * 2
        int n = cap - 1;
        //从最高位的1往低位复制
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        //到这里，从最高位的1到第0位都是1了，再加上1就是2的次幂
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    private String shardingKeyToCollectionName(Object shardValue) {
        Optional.ofNullable(shardValue).orElseThrow(() -> new IllegalArgumentException("shard value shouldn't be empty !"));
        int hashCode = shardValue.hashCode();
        int tableSequence = hashCode & shardingCount - 1;
        return parseSequenceToCollectionName(tableSequence);
    }

    private String parseSequenceToCollectionName(int sequence) {
        return collectionName + "_" + sequence;
    }

    protected String getCollectionNameFromQuery(Query query) {
        Object shardValue = query.getQueryObject().get(shardingColumn);
        if (shardValue == null) {
            return null;
        }
        return shardingKeyToCollectionName(shardValue);
    }

    protected String getCollectionNameFromEntity(T entity) {
        Object shardValue = null;
        try {
            shardValue = FieldUtils.readField(shardingField, entity, true);
            if (shardValue == null) {
                return null;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("got IllegalAccessException when reading shard value !");
        }
        return shardingKeyToCollectionName(shardValue);
    }

    @Override
    public String insert(T entity) {
        getMongoTemplate().insert(entity, getCollectionNameFromEntity(entity));
        return entity.getId();
    }

    @Override
    public List<T> find(Query query, Class<T> clazz) {
        return getMongoTemplate().find(query, clazz, getCollectionNameFromQuery(query));
    }

    @Override
    public void delete(Query query) {
        String collectionNmae = getCollectionNameFromQuery(query);
        if (collectionNmae != null) {
            getMongoTemplate().remove(query, classType, collectionNmae);
            return;
        }
        for (int i = 0, len = shardingCount; i < len; i++) {
            getMongoTemplate().remove(query, classType, parseSequenceToCollectionName(i));
        }
    }


    @Override
    public void updateMulti(Query query, Update update) {
        String collectionNmae = getCollectionNameFromQuery(query);
        if (collectionNmae != null) {
            getMongoTemplate().updateMulti(query, update, collectionNmae);
            return;
        }
        for (int i = 0, len = shardingCount; i < len; i++) {
            getMongoTemplate().updateMulti(query, update, parseSequenceToCollectionName(i));
        }
    }
}
