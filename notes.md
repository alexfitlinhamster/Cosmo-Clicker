Задача:
--------- beginning of crash
1970-10-22 15:57:37.326   574-574   libc                    pid-574                              A  Fatal signal 6 (SIGABRT), code -1 (SI_QUEUE) in tid 574 (init), pid 574 (init)
--------- beginning of system
2026-07-22 09:34:12.888  1682-1682  libc                    pid-1682                             A  Fatal signal 6 (SIGABRT), code -1 (SI_QUEUE) in tid 1682 (init), pid 1682 (init)
2026-07-22 09:34:34.963  4121-4121  libc                    pid-4121                             A  Fatal signal 6 (SIGABRT), code -1 (SI_QUEUE) in tid 4121 (init), pid 4121 (init)
2026-07-22 15:04:25.909 32266-32266 AndroidRuntime          com.example.myapplication            E  FATAL EXCEPTION: main (Fix with AI)
Process: com.example.myapplication, PID: 32266
androidx.compose.ui.res.ResourceResolutionException: Error attempting to load resource: res/drawable/upgrade_debris_harvester.png
at androidx.compose.ui.res.PainterResources_androidKt.loadImageBitmapResource(PainterResources.android.kt:112)
at androidx.compose.ui.res.PainterResources_androidKt.painterResource(PainterResources.android.kt:72)
at com.example.myapplication.ui.components.ShopBarKt.ShopRow(ShopBar.kt:276)
at com.example.myapplication.ui.components.ShopBarKt$ShopBar$lambda$29$lambda$28$lambda$27$lambda$26$$inlined$items$default$12.invoke(LazyDsl.kt:526)
at com.example.myapplication.ui.components.ShopBarKt$ShopBar$lambda$29$lambda$28$lambda$27$lambda$26$$inlined$items$default$12.invoke(LazyDsl.kt:178)
at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.kt:143)
at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.kt:52)
at androidx.compose.foundation.lazy.LazyListItemProviderImpl.Item$lambda$0(LazyListItemProvider.kt:80)
at androidx.compose.foundation.lazy.LazyListItemProviderImpl$$ExternalSyntheticLambda0.invoke(D8$$SyntheticClass:0)
at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.kt:122)
at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.kt:52)
at androidx.compose.runtime.CompositionLocalKt.CompositionLocalProvider(CompositionLocal.kt:398)
at androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItemKt.LazyLayoutPinnableItem(LazyLayoutPinnableItem.kt:56)
at androidx.compose.foundation.lazy.LazyListItemProviderImpl.Item(LazyListItemProvider.kt:78)
at androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactoryKt.SkippableItem_JVlU9Rs$lambda$0(LazyLayoutItemContentFactory.kt:127)
at androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactoryKt$$ExternalSyntheticLambda0.invoke(D8$$SyntheticClass:0)
at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.kt:122)
at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.kt:52)
at androidx.compose.runtime.CompositionLocalKt.CompositionLocalProvider(CompositionLocal.kt:378)
at androidx.compose.runtime.saveable.SaveableStateHolderImpl.SaveableStateProvider(SaveableStateHolder.kt:82)
at androidx.compose.foundation.lazy.layout.LazySaveableStateHolder.SaveableStateProvider(LazySaveableStateHolder.kt:76)
at androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactoryKt.SkippableItem-JVlU9Rs(LazyLayoutItemContentFactory.kt:126)
at androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactoryKt.access$SkippableItem-JVlU9Rs(LazyLayoutItemContentFactory.kt:1)
at androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactory$CachedItemContent.createContentLambda$lambda$0(LazyLayoutItemContentFactory.kt:95)
at androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactory$CachedItemContent$$ExternalSyntheticLambda0.invoke(D8$$SyntheticClass:0)
at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.kt:122)
at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.kt:52)
at androidx.compose.runtime.internal.Expect_jvmKt.invokeComposable(Expect.jvm.kt:24)
at androidx.compose.runtime.ComposerImpl.doCompose-aFTiNEg(ComposerImpl.kt:2647)
at androidx.compose.runtime.ComposerImpl.composeContent--ZbOJvo$runtime(ComposerImpl.kt:2551)
at androidx.compose.runtime.CompositionImpl.composeContent(Composition.kt:835)
at androidx.compose.runtime.Recomposer.composeInitial$runtime(Recomposer.kt:1266)
at androidx.compose.runtime.ComposerImpl$CompositionContextImpl.composeInitial$runtime(ComposerImpl.kt:2993)
at androidx.compose.runtime.CompositionImpl.composeInitial(Composition.kt:672)
at androidx.compose.runtime.CompositionImpl.composeInitialWithReuse(Composition.kt:699)
--------- beginning of main

Drone Salvage наща названия

баги в магазин в отделе клики при листание вниз игар вылетает исправь 