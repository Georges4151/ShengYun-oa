# 现代化UI/UX设计系统 - 快速开始指南

## 🚀 快速开始

### 1. 引入CSS文件

在FXML文件中引入新的设计系统CSS：

```xml
<stylesheets>
    <URL value="@../css/design-system.css" />
    <URL value="@../css/components.css" />
    <URL value="@../css/layout.css" />
    <URL value="@../css/responsive.css" />
    <URL value="@../css/performance.css" />
</stylesheets>
```

### 2. 使用设计变量

#### 色彩变量
```css
/* 主色调 */
-fx-background-color: var(--primary-600);
-fx-text-fill: var(--primary-700);

/* 语义色彩 */
-fx-background-color: var(--success-500);
-fx-text-fill: var(--error-600);

/* 中性色 */
-fx-background-color: var(--gray-50);
-fx-text-fill: var(--gray-900);
```

#### 间距变量
```css
-fx-padding: var(--space-4);      /* 16px */
-fx-spacing: var(--space-6);     /* 24px */
-fx-margin: var(--space-8);      /* 32px */
```

#### 圆角变量
```css
-fx-background-radius: var(--radius-lg);   /* 8px */
-fx-border-radius: var(--radius-xl);      /* 12px */
```

#### 阴影变量
```css
-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 0, 4px, 0, 8);
```

## 🎨 常用组件示例

### 按钮组件

```xml
<!-- 主要按钮 -->
<Button text="提交" styleClass="button" />

<!-- 次要按钮 -->
<Button text="取消" styleClass="button-secondary" />

<!-- 危险按钮 -->
<Button text="删除" styleClass="button-danger" />

<!-- 成功按钮 -->
<Button text="保存" styleClass="button-success" />

<!-- 警告按钮 -->
<Button text="警告" styleClass="button-warning" />

<!-- 幽灵按钮 -->
<Button text="查看详情" styleClass="button-ghost" />

<!-- 链接按钮 -->
<Button text="了解更多" styleClass="button-link" />

<!-- 小按钮 -->
<Button text="确定" styleClass="button-sm" />

<!-- 大按钮 -->
<Button text="立即开始" styleClass="button-lg" />
```

### 卡片组件

```xml
<!-- 基础卡片 -->
<VBox styleClass="card" spacing="16.0">
    <Label text="卡片标题" styleClass="card-title" />
    <Label text="卡片内容" styleClass="card-content" />
</VBox>

<!-- 带头部的卡片 -->
<VBox styleClass="card">
    <VBox styleClass="card-header" spacing="8.0">
        <Label text="标题" styleClass="card-title" />
        <Label text="副标题" styleClass="card-subtitle" />
    </VBox>
    <VBox styleClass="card-body" spacing="12.0">
        <Label text="内容区域" />
    </VBox>
    <HBox styleClass="card-footer" spacing="8.0">
        <Button text="操作1" styleClass="button-secondary" />
        <Button text="操作2" styleClass="button" />
    </HBox>
</VBox>

<!-- 信息卡片 -->
<VBox styleClass="info-card, info-card-primary" spacing="12.0">
    <HBox spacing="12.0" alignment="CENTER_LEFT">
        <VBox styleClass="icon-container, icon-container-primary">
            <Label text="📊" styleClass="icon-text" />
        </VBox>
        <VBox spacing="4.0">
            <Label text="项目总数" styleClass="info-card-label" />
            <Label text="100" styleClass="info-card-value" />
        </VBox>
    </HBox>
    <Label text="较上月 +12%" styleClass="info-card-trend, trend-up" />
</VBox>
```

### 表单组件

```xml
<!-- 文本输入框 -->
<VBox styleClass="form-group" spacing="8.0">
    <Label text="用户名" styleClass="form-label" />
    <TextField promptText="请输入用户名" styleClass="text-field" />
</VBox>

<!-- 密码输入框 -->
<VBox styleClass="form-group" spacing="8.0">
    <Label text="密码" styleClass="form-label" />
    <PasswordField promptText="请输入密码" styleClass="password-field" />
</VBox>

<!-- 文本区域 -->
<VBox styleClass="form-group" spacing="8.0">
    <Label text="描述" styleClass="form-label" />
    <TextArea promptText="请输入描述" styleClass="text-area" />
</VBox>

<!-- 下拉选择框 -->
<VBox styleClass="form-group" spacing="8.0">
    <Label text="类型" styleClass="form-label" />
    <ComboBox promptText="请选择类型" styleClass="combo-box" />
</VBox>

<!-- 日期选择器 -->
<VBox styleClass="form-group" spacing="8.0">
    <Label text="日期" styleClass="form-label" />
    <DatePicker promptText="请选择日期" styleClass="date-picker" />
</VBox>

<!-- 复选框 -->
<HBox spacing="8.0" alignment="CENTER_LEFT">
    <CheckBox text="记住我" styleClass="check-box" />
</HBox>

<!-- 单选按钮组 -->
<VBox spacing="8.0">
    <RadioButton text="选项1" styleClass="radio-button" />
    <RadioButton text="选项2" styleClass="radio-button" />
    <RadioButton text="选项3" styleClass="radio-button" />
</VBox>
```

### 表格组件

```xml
<!-- 基础表格 -->
<TableView styleClass="table-view">
    <columns>
        <TableColumn text="ID" prefWidth="80.0" />
        <TableColumn text="名称" prefWidth="200.0" />
        <TableColumn text="状态" prefWidth="100.0" />
        <TableColumn text="操作" prefWidth="150.0" />
    </columns>
</TableView>

<!-- 响应式表格 -->
<TableView styleClass="table-view, responsive-table">
    <columns>
        <TableColumn text="ID" prefWidth="80.0" />
        <TableColumn text="名称" prefWidth="200.0" />
        <TableColumn text="状态" prefWidth="100.0" />
    </columns>
</TableView>
```

### 状态标签

```xml
<!-- 主要标签 -->
<Label text="进行中" styleClass="status-badge, status-badge-primary" />

<!-- 成功标签 -->
<Label text="已完成" styleClass="status-badge, status-badge-success" />

<!-- 警告标签 -->
<Label text="待处理" styleClass="status-badge, status-badge-warning" />

<!-- 错误标签 -->
<Label text="已失败" styleClass="status-badge, status-badge-error" />

<!-- 信息标签 -->
<Label text="新消息" styleClass="status-badge, status-badge-info" />
```

### 提示框

```xml
<!-- 主要提示 -->
<VBox styleClass="alert, alert-primary" spacing="12.0">
    <Label text="ℹ️" styleClass="alert-icon" />
    <VBox spacing="4.0">
        <Label text="提示标题" styleClass="alert-title" />
        <Label text="提示内容" styleClass="alert-message" />
    </VBox>
</VBox>

<!-- 成功提示 -->
<VBox styleClass="alert, alert-success" spacing="12.0">
    <Label text="✓" styleClass="alert-icon" />
    <VBox spacing="4.0">
        <Label text="操作成功" styleClass="alert-title" />
        <Label text="您的操作已成功完成" styleClass="alert-message" />
    </VBox>
</VBox>

<!-- 警告提示 -->
<VBox styleClass="alert, alert-warning" spacing="12.0">
    <Label text="⚠️" styleClass="alert-icon" />
    <VBox spacing="4.0">
        <Label text="警告" styleClass="alert-title" />
        <Label text="请注意此操作的影响" styleClass="alert-message" />
    </VBox>
</VBox>

<!-- 错误提示 -->
<VBox styleClass="alert, alert-error" spacing="12.0">
    <Label text="✕" styleClass="alert-icon" />
    <VBox spacing="4.0">
        <Label text="错误" styleClass="alert-title" />
        <Label text="操作失败，请重试" styleClass="alert-message" />
    </VBox>
</VBox>
```

## 📐 布局示例

### 页面布局

```xml
<BorderPane styleClass="root">
    <stylesheets>
        <URL value="@../css/design-system.css" />
        <URL value="@../css/layout.css" />
    </stylesheets>
    
    <!-- 侧边栏 -->
    <left>
        <VBox styleClass="sidebar" prefWidth="260.0">
            <!-- 侧边栏内容 -->
        </VBox>
    </left>
    
    <!-- 主内容区域 -->
    <center>
        <VBox styleClass="content-container">
            <!-- 页面内容 -->
        </VBox>
    </center>
</BorderPane>
```

### 容器布局

```xml
<!-- 基础容器 -->
<VBox styleClass="container" spacing="16.0">
    <Label text="容器内容" />
</VBox>

<!-- 流式容器 -->
<VBox styleClass="container-fluid">
    <Label text="流式容器内容" />
</VBox>
```

### 网格布局

```xml
<!-- 2列网格 -->
<HBox styleClass="grid-container, grid-cols-2" spacing="24.0">
    <VBox styleClass="grid-item">
        <Label text="列1内容" />
    </VBox>
    <VBox styleClass="grid-item">
        <Label text="列2内容" />
    </VBox>
</HBox>

<!-- 3列网格 -->
<HBox styleClass="grid-container, grid-cols-3" spacing="24.0">
    <VBox styleClass="grid-item">
        <Label text="列1内容" />
    </VBox>
    <VBox styleClass="grid-item">
        <Label text="列2内容" />
    </VBox>
    <VBox styleClass="grid-item">
        <Label text="列3内容" />
    </VBox>
</HBox>
```

## ✨ 动画效果

### 淡入动画

```xml
<VBox styleClass="fade-in">
    <Label text="淡入效果" />
</VBox>
```

### 滑入动画

```xml
<VBox styleClass="slide-in-up">
    <Label text="从下滑入" />
</VBox>

<VBox styleClass="slide-in-down">
    <Label text="从上滑入" />
</VBox>

<VBox styleClass="slide-in-left">
    <Label text="从左滑入" />
</VBox>

<VBox styleClass="slide-in-right">
    <Label text="从右滑入" />
</VBox>
```

### 缩放动画

```xml
<VBox styleClass="scale-in">
    <Label text="缩放进入" />
</VBox>
```

### 脉冲动画

```xml
<Label text="脉冲效果" styleClass="pulse" />
```

## 🎯 实用工具类

### 间距工具类

```xml
<!-- 外边距 -->
<VBox styleClass="mt-4">   <!-- 上边距 16px -->
    <Label text="内容" />
</VBox>

<VBox styleClass="mb-4">   <!-- 下边距 16px -->
    <Label text="内容" />
</VBox>

<!-- 内边距 -->
<VBox styleClass="p-4">    <!-- 内边距 16px -->
    <Label text="内容" />
</VBox>

<VBox styleClass="p-6">    <!-- 内边距 24px -->
    <Label text="内容" />
</VBox>
```

### 文本工具类

```xml
<!-- 文本对齐 -->
<Label text="居中文本" styleClass="text-center" />
<Label text="左对齐文本" styleClass="text-left" />
<Label text="右对齐文本" styleClass="text-right" />

<!-- 字体粗细 -->
<Label text="粗体文本" styleClass="font-bold" />
<Label text="半粗体文本" styleClass="font-semibold" />
<Label text="中等粗细文本" styleClass="font-medium" />

<!-- 文本颜色 -->
<Label text="主要文本" styleClass="text-primary" />
<Label text="成功文本" styleClass="text-success" />
<Label text="警告文本" styleClass="text-warning" />
<Label text="错误文本" styleClass="text-error" />
<Label text="信息文本" styleClass="text-info" />
```

### 圆角工具类

```xml
<VBox styleClass="rounded">     <!-- 6px圆角 -->
    <Label text="内容" />
</VBox>

<VBox styleClass="rounded-lg">  <!-- 8px圆角 -->
    <Label text="内容" />
</VBox>

<VBox styleClass="rounded-xl">  <!-- 12px圆角 -->
    <Label text="内容" />
</VBox>

<VBox styleClass="rounded-full"> <!-- 完全圆角 -->
    <Label text="内容" />
</VBox>
```

### 阴影工具类

```xml
<VBox styleClass="shadow">     <!-- 小阴影 -->
    <Label text="内容" />
</VBox>

<VBox styleClass="shadow-lg">  <!-- 大阴影 -->
    <Label text="内容" />
</VBox>

<VBox styleClass="shadow-xl">  <!-- 超大阴影 -->
    <Label text="内容" />
</VBox>
```

## 📱 响应式设计

### 响应式容器

```xml
<VBox styleClass="responsive-container">
    <Label text="响应式内容" />
</VBox>
```

### 响应式表单

```xml
<VBox styleClass="responsive-form" spacing="12.0">
    <TextField promptText="响应式输入框" styleClass="text-field" />
    <ComboBox promptText="响应式下拉框" styleClass="combo-box" />
</VBox>
```

### 响应式按钮组

```xml
<HBox styleClass="button-group" spacing="8.0">
    <Button text="按钮1" styleClass="button" />
    <Button text="按钮2" styleClass="button" />
    <Button text="按钮3" styleClass="button" />
</HBox>
```

## 🎭 主题切换

### 深色模式

```xml
<BorderPane styleClass="root, dark-mode">
    <!-- 深色模式内容 -->
</BorderPane>
```

### 高对比度模式

```xml
<BorderPane styleClass="root, high-contrast">
    <!-- 高对比度内容 -->
</BorderPane>
```

### 减少动画

```xml
<VBox styleClass="fade-in, reduced-motion">
    <Label text="减少动画效果" />
</VBox>
```

## 🔧 性能优化

### 硬件加速

```xml
<VBox styleClass="gpu-accelerated">
    <Label text="硬件加速内容" />
</VBox>
```

### 懒加载

```xml
<VBox styleClass="lazy-load">
    <Label text="懒加载内容" />
</VBox>
```

### 虚拟滚动

```xml
<TableView styleClass="virtual-scroll">
    <!-- 大数据表格 -->
</TableView>
```

## 📋 最佳实践

### 1. 使用语义化样式

```xml
<!-- 推荐 -->
<VBox styleClass="card">
    <Label text="标题" styleClass="card-title" />
</VBox>

<!-- 不推荐 -->
<VBox style="-fx-background-color: white; -fx-border-radius: 8px;">
    <Label text="标题" style="-fx-font-size: 16px; -fx-font-weight: bold;" />
</VBox>
```

### 2. 使用设计变量

```css
/* 推荐 */
.button {
    -fx-background-color: var(--primary-600);
}

/* 不推荐 */
.button {
    -fx-background-color: #1a6df5;
}
```

### 3. 保持一致性

```xml
<!-- 在整个应用中使用相同的组件和样式 -->
<Button text="提交" styleClass="button" />
<Button text="保存" styleClass="button" />
<Button text="删除" styleClass="button-danger" />
```

### 4. 优化性能

```xml
<!-- 使用硬件加速 -->
<VBox styleClass="gpu-accelerated">
    <!-- 复杂组件 -->
</VBox>

<!-- 使用懒加载 -->
<VBox styleClass="lazy-load">
    <!-- 非关键内容 -->
</VBox>
```

### 5. 确保可访问性

```xml
<!-- 提供清晰的焦点状态 -->
<TextField styleClass="text-field" />

<!-- 提供足够的对比度 -->
<Label text="重要文本" styleClass="text-primary" />

<!-- 支持键盘导航 -->
<Button text="操作" styleClass="button" />
```

## 🐛 常见问题

### Q: 如何自定义颜色？

A: 修改 `design-system.css` 中的CSS变量：

```css
:root {
    --primary-600: #your-color;
}
```

### Q: 如何调整组件大小？

A: 使用尺寸工具类或修改组件样式：

```xml
<Button text="按钮" styleClass="button-sm" />
```

### Q: 如何禁用动画？

A: 添加 `reduced-motion` 类：

```xml
<VBox styleClass="fade-in, reduced-motion">
    <Label text="无动画内容" />
</VBox>
```

### Q: 如何实现深色模式？

A: 添加 `dark-mode` 类到根元素：

```xml
<BorderPane styleClass="root, dark-mode">
    <!-- 深色模式内容 -->
</BorderPane>
```

## 📚 更多资源

- [完整设计文档](./UI_UX_DESIGN_GUIDE.md)
- [组件库文档](./COMPONENTS.md)
- [响应式设计指南](./RESPONSIVE.md)
- [性能优化指南](./PERFORMANCE.md)
- [可访问性指南](./ACCESSIBILITY.md)

## 💡 提示

1. **优先使用组件类** - 使用预定义的组件类而不是自定义样式
2. **遵循设计规范** - 保持与设计系统的一致性
3. **测试响应式** - 在不同设备上测试布局
4. **优化性能** - 使用性能优化技巧
5. **确保可访问性** - 遵循WCAG标准

---

祝您使用愉快！如有问题，请参考完整文档或联系设计团队。