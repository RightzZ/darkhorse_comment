package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private IShopTypeService typeService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result typeList() {
        String key = CACHE_SHOP_TYPE_KEY;
        // 从redis查询商铺列表信息
        List<String> cacheList = stringRedisTemplate.opsForList().range(key, 0, -1);

        // 判断是否存在
        if (cacheList!=null && !cacheList.isEmpty()) {
            // 存在，返回
            List<ShopType> typeList = cacheList.stream()
                    .map(json -> JSONUtil.toBean(json, ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(typeList);
        }

        // 不存在，查询数据库
        List<ShopType> typeList = typeService
                .query().orderByAsc("sort").list();

        // 数据库里不存在，返回错误
        if (typeList==null || typeList.isEmpty()) {
            return Result.fail("商铺类型不存在！");
        }

        // 存在，将商铺列表信息写入redis，以list形式
        List<String> jsonList = typeList.stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(key, jsonList);

        // 返回
        return Result.ok(typeList);
    }
}
