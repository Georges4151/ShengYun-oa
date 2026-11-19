package com.ceams.module.integration.client;

/**
 * 企业微信部门。
 *
 * @param id 部门 ID
 * @param name 部门名称
 * @param parentId 上级部门 ID（根部门为 1）
 * @param order 同级排序号
 */
public record WeComDept(Long id, String name, Long parentId, Integer order) {}
