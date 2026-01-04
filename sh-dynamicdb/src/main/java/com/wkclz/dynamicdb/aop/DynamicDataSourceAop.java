package com.wkclz.dynamicdb.aop;


import com.wkclz.dynamicdb.DynamicDataSourceHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author shrimp @ 2019-07-28 23:56:25
 */
@Aspect
@Component
public class DynamicDataSourceAop {

    private static final String POINT_CUT = "@within(org.apache.ibatis.annotations.Mapper)";

    @Pointcut(POINT_CUT)
    public void pointCut() {
    }

    @Around(value = POINT_CUT)
    public Object doAroundAdvice(ProceedingJoinPoint point) throws Throwable {
        try {
            return point.proceed();
        } finally {
            DynamicDataSourceHolder.clear();
        }
    }

}
