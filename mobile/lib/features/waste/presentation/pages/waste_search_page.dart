import 'package:flutter/material.dart';
import 'package:echosnap/app/app_routes.dart';
import 'package:echosnap/core/storage/token_storage.dart';
import 'package:echosnap/core/theme/app_theme.dart';
import 'package:echosnap/features/waste/data/waste_search_api.dart';

class WasteSearchPage extends StatefulWidget {
  const WasteSearchPage({
    super.key,
    this.selectionMode = false,
  });

  /// false:
  /// 일반 품목 검색
  /// -> 품목 상세 페이지 이동
  ///
  /// true:
  /// AI 정정용 품목 선택
  /// -> 선택된 WasteSearchItem을 pop으로 반환
  final bool selectionMode;

  @override
  State<WasteSearchPage> createState() =>
      _WasteSearchPageState();
}

class _WasteSearchPageState
    extends State<WasteSearchPage> {
  final TextEditingController
  _searchController =
  TextEditingController();

  List<WasteCategoryItem> _categories =
  const [];

  List<WasteSearchItem> _items =
  const [];

  int? _selectedCategoryId;

  int _currentPage = 0;

  int _totalElements = 0;

  bool _isLoading = true;

  bool _isLoadingMore = false;

  bool _isLastPage = true;

  String? _errorMessage;

  @override
  void initState() {
    super.initState();

    _initialize();
  }

  @override
  void dispose() {
    _searchController.dispose();

    super.dispose();
  }

  Future<void> _initialize() async {
    await _loadCategories();

    if (!mounted) {
      return;
    }

    await _search(
      reset: true,
    );
  }

  Future<void> _loadCategories() async {
    try {
      final List<WasteCategoryItem>
      categories =
      await WasteSearchApi
          .getCategories();

      if (!mounted) {
        return;
      }

      setState(() {
        _categories =
            categories;
      });
    } on WasteSearchApiException catch (
    exception
    ) {
      if (!mounted) {
        return;
      }

      if (exception.unauthorized) {
        await _moveToLogin();

        return;
      }

      _showMessage(
        exception.message,
      );
    }
  }

  Future<void> _search({
    required bool reset,
  }) async {
    if (_isLoadingMore) {
      return;
    }

    if (!reset &&
        _isLastPage) {
      return;
    }

    final int nextPage =
    reset
        ? 0
        : _currentPage + 1;

    if (reset) {
      setState(() {
        _isLoading = true;

        _errorMessage = null;
      });
    } else {
      setState(() {
        _isLoadingMore =
        true;
      });
    }

    try {
      final WasteSearchResult result =
      await WasteSearchApi
          .searchItems(
        keyword:
        _searchController.text,
        page:
        nextPage,
        categoryId:
        _selectedCategoryId,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        if (reset) {
          _items =
              result.items;
        } else {
          _items = [
            ..._items,
            ...result.items,
          ];
        }

        _currentPage =
            result.page;

        _totalElements =
            result.totalElements;

        _isLastPage =
            result.last;

        _errorMessage =
        null;
      });
    } on WasteSearchApiException catch (
    exception
    ) {
      if (!mounted) {
        return;
      }

      if (exception.unauthorized) {
        await _moveToLogin();

        return;
      }

      if (reset) {
        setState(() {
          _items =
          const [];

          _totalElements =
          0;

          _errorMessage =
              exception.message;
        });
      } else {
        _showMessage(
          exception.message,
        );
      }
    } catch (_) {
      if (!mounted) {
        return;
      }

      setState(() {
        _errorMessage =
        '품목 정보를 불러오는 중 '
            '오류가 발생했습니다.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading =
          false;

          _isLoadingMore =
          false;
        });
      }
    }
  }

  Future<void> _selectCategory(
      int? categoryId,
      ) async {
    if (_selectedCategoryId ==
        categoryId) {
      return;
    }

    setState(() {
      _selectedCategoryId =
          categoryId;
    });

    await _search(
      reset: true,
    );
  }

  Future<void> _moveToLogin() async {
    await TokenStorage.clearTokens();

    if (!mounted) {
      return;
    }

    Navigator.pushNamedAndRemoveUntil(
      context,
      AppRoutes.login,
          (route) => false,
    );
  }

  void _showMessage(
      String message,
      ) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            message,
          ),
        ),
      );
  }

  void _openItem(
      WasteSearchItem item,
      ) {
    /*
     * AI 정정 화면에서 들어온 경우에는
     * 상세 화면으로 이동하지 않고
     * 사용자가 선택한 품목을
     * 이전 화면에 그대로 반환합니다.
     */
    if (widget.selectionMode) {
      Navigator.pop(
        context,
        item,
      );

      return;
    }

    /*
     * 일반 검색 화면에서는
     * 기존 동작을 그대로 유지합니다.
     */
    Navigator.pushNamed(
      context,
      AppRoutes.wasteDetail,
      arguments:
      item.id,
    );
  }

  @override
  Widget build(
      BuildContext context,
      ) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.selectionMode
              ? '올바른 품목 선택'
              : '품목 검색',
        ),
      ),
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            if (widget.selectionMode)
              Container(
                width:
                double.infinity,
                margin:
                const EdgeInsets.fromLTRB(
                  20,
                  8,
                  20,
                  4,
                ),
                padding:
                const EdgeInsets.all(
                  15,
                ),
                decoration:
                BoxDecoration(
                  color: AppTheme
                      .primaryColor
                      .withValues(
                    alpha: 0.08,
                  ),
                  borderRadius:
                  BorderRadius.circular(
                    16,
                  ),
                ),
                child: const Row(
                  crossAxisAlignment:
                  CrossAxisAlignment.start,
                  children: [
                    Icon(
                      Icons
                          .fact_check_outlined,
                      color: AppTheme
                          .primaryColor,
                      size: 21,
                    ),
                    SizedBox(
                      width: 10,
                    ),
                    Expanded(
                      child: Text(
                        'AI 추정과 다른 실제 품목을 '
                            '선택해주세요. 선택한 결과는 '
                            '더 정확한 AI 개선을 위한 '
                            '검수 자료로 활용할 수 있어요.',
                        style:
                        TextStyle(
                          fontSize: 12,
                          height: 1.5,
                        ),
                      ),
                    ),
                  ],
                ),
              ),

            Padding(
              padding:
              const EdgeInsets.fromLTRB(
                20,
                8,
                20,
                0,
              ),
              child: TextField(
                controller:
                _searchController,
                textInputAction:
                TextInputAction.search,
                onSubmitted: (_) {
                  _search(
                    reset: true,
                  );
                },
                decoration:
                InputDecoration(
                  hintText:
                  '예: 페트병, 종이박스, 캔',
                  prefixIcon:
                  const Icon(
                    Icons.search_rounded,
                  ),
                  suffixIcon:
                  IconButton(
                    tooltip:
                    '검색',
                    onPressed: () {
                      _search(
                        reset: true,
                      );
                    },
                    icon:
                    const Icon(
                      Icons
                          .arrow_forward_rounded,
                    ),
                  ),
                ),
              ),
            ),

            const SizedBox(
              height: 16,
            ),

            if (_categories.isNotEmpty)
              SizedBox(
                height: 42,
                child: ListView(
                  scrollDirection:
                  Axis.horizontal,
                  padding:
                  const EdgeInsets.symmetric(
                    horizontal: 20,
                  ),
                  children: [
                    _CategoryChip(
                      label:
                      '전체',
                      selected:
                      _selectedCategoryId ==
                          null,
                      onTap: () {
                        _selectCategory(
                          null,
                        );
                      },
                    ),

                    const SizedBox(
                      width: 8,
                    ),

                    ..._categories.map(
                          (
                          WasteCategoryItem
                          category,
                          ) {
                        return Padding(
                          padding:
                          const EdgeInsets.only(
                            right: 8,
                          ),
                          child:
                          _CategoryChip(
                            label:
                            category.name,
                            selected:
                            _selectedCategoryId ==
                                category.id,
                            onTap: () {
                              _selectCategory(
                                category.id,
                              );
                            },
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),

            const SizedBox(
              height: 16,
            ),

            Expanded(
              child:
              _buildContent(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    if (_isLoading) {
      return const Center(
        child:
        CircularProgressIndicator(),
      );
    }

    if (_errorMessage != null) {
      return Center(
        child: Padding(
          padding:
          const EdgeInsets.all(
            32,
          ),
          child: Column(
            mainAxisSize:
            MainAxisSize.min,
            children: [
              const Icon(
                Icons
                    .error_outline_rounded,
                size: 46,
                color: AppTheme
                    .textSecondaryColor,
              ),

              const SizedBox(
                height: 14,
              ),

              Text(
                _errorMessage!,
                textAlign:
                TextAlign.center,
              ),

              const SizedBox(
                height: 18,
              ),

              ElevatedButton(
                onPressed: () {
                  _search(
                    reset: true,
                  );
                },
                child: const Text(
                  '다시 시도',
                ),
              ),
            ],
          ),
        ),
      );
    }

    if (_items.isEmpty) {
      final String keyword =
      _searchController.text
          .trim();

      return Center(
        child: Padding(
          padding:
          const EdgeInsets.all(
            32,
          ),
          child: Column(
            mainAxisSize:
            MainAxisSize.min,
            children: [
              Container(
                width: 64,
                height: 64,
                decoration:
                BoxDecoration(
                  color: AppTheme
                      .primaryColor
                      .withValues(
                    alpha: 0.08,
                  ),
                  borderRadius:
                  BorderRadius.circular(
                    20,
                  ),
                ),
                child: const Icon(
                  Icons
                      .search_off_rounded,
                  size: 32,
                  color: AppTheme
                      .primaryColor,
                ),
              ),

              const SizedBox(
                height: 18,
              ),

              Text(
                keyword.isEmpty
                    ? '등록된 품목이 없습니다.'
                    : '\'$keyword\' 검색 결과가 없습니다.',
                textAlign:
                TextAlign.center,
                style:
                Theme.of(
                  context,
                )
                    .textTheme
                    .titleMedium,
              ),

              const SizedBox(
                height: 8,
              ),

              Text(
                '다른 품목명이나 '
                    '키워드로 검색해보세요.',
                textAlign:
                TextAlign.center,
                style:
                Theme.of(
                  context,
                )
                    .textTheme
                    .bodyMedium,
              ),
            ],
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () {
        return _search(
          reset: true,
        );
      },
      child: ListView(
        padding:
        const EdgeInsets.fromLTRB(
          20,
          0,
          20,
          28,
        ),
        children: [
          Text(
            widget.selectionMode
                ? '올바른 품목을 눌러 선택해주세요.'
                : '총 $_totalElements개의 품목',
            style:
            Theme.of(
              context,
            )
                .textTheme
                .bodyMedium
                ?.copyWith(
              fontSize: 13,
            ),
          ),

          const SizedBox(
            height: 12,
          ),

          ..._items.map(
                (
                WasteSearchItem item,
                ) {
              return Padding(
                padding:
                const EdgeInsets.only(
                  bottom: 10,
                ),
                child: Card(
                  child: InkWell(
                    onTap: () {
                      _openItem(
                        item,
                      );
                    },
                    borderRadius:
                    BorderRadius.circular(
                      20,
                    ),
                    child: Padding(
                      padding:
                      const EdgeInsets.all(
                        16,
                      ),
                      child: Row(
                        children: [
                          _WasteImage(
                            imageUrl:
                            item.imageUrl,
                          ),

                          const SizedBox(
                            width: 16,
                          ),

                          Expanded(
                            child: Column(
                              crossAxisAlignment:
                              CrossAxisAlignment
                                  .start,
                              children: [
                                Text(
                                  item.name,
                                  style:
                                  Theme.of(
                                    context,
                                  )
                                      .textTheme
                                      .titleMedium,
                                ),

                                const SizedBox(
                                  height: 5,
                                ),

                                Text(
                                  item.category
                                      .name,
                                  style:
                                  Theme.of(
                                    context,
                                  )
                                      .textTheme
                                      .bodyMedium
                                      ?.copyWith(
                                    fontSize: 13,
                                  ),
                                ),
                              ],
                            ),
                          ),

                          Icon(
                            widget.selectionMode
                                ? Icons
                                .check_circle_outline_rounded
                                : Icons
                                .chevron_right_rounded,
                            color: widget
                                .selectionMode
                                ? AppTheme
                                .primaryColor
                                : AppTheme
                                .textSecondaryColor,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            },
          ),

          if (!_isLastPage)
            Padding(
              padding:
              const EdgeInsets.only(
                top: 8,
              ),
              child:
              OutlinedButton(
                onPressed:
                _isLoadingMore
                    ? null
                    : () {
                  _search(
                    reset: false,
                  );
                },
                child:
                _isLoadingMore
                    ? const SizedBox(
                  width: 20,
                  height: 20,
                  child:
                  CircularProgressIndicator(
                    strokeWidth:
                    2,
                  ),
                )
                    : const Text(
                  '더 보기',
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _CategoryChip
    extends StatelessWidget {
  const _CategoryChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;

  final bool selected;

  final VoidCallback onTap;

  @override
  Widget build(
      BuildContext context,
      ) {
    return ChoiceChip(
      label: Text(
        label,
      ),
      selected:
      selected,
      onSelected: (_) {
        onTap();
      },
      selectedColor:
      AppTheme.primaryColor
          .withValues(
        alpha: 0.12,
      ),
      showCheckmark:
      false,
      side: BorderSide(
        color: selected
            ? AppTheme
            .primaryColor
            : const Color(
          0xFFDCE4E0,
        ),
      ),
      labelStyle:
      TextStyle(
        color: selected
            ? AppTheme
            .primaryColor
            : AppTheme
            .textPrimaryColor,
        fontWeight:
        selected
            ? FontWeight.w700
            : FontWeight.w500,
      ),
    );
  }
}

class _WasteImage extends StatelessWidget {
  const _WasteImage({
    required this.imageUrl,
  });

  final String? imageUrl;

  @override
  Widget build(
      BuildContext context,
      ) {
    final String? url =
    imageUrl?.trim();

    return Container(
      width: 58,
      height: 58,
      clipBehavior:
      Clip.antiAlias,
      decoration:
      BoxDecoration(
        color: AppTheme
            .primaryColor
            .withValues(
          alpha: 0.08,
        ),
        borderRadius:
        BorderRadius.circular(
          16,
        ),
      ),
      child:
      url == null ||
          url.isEmpty
          ? const Icon(
        Icons
            .recycling_rounded,
        color: AppTheme
            .primaryColor,
        size: 28,
      )
          : Image.network(
        url,
        fit:
        BoxFit.cover,
        errorBuilder: (
            context,
            error,
            stackTrace,
            ) {
          return const Icon(
            Icons
                .recycling_rounded,
            color: AppTheme
                .primaryColor,
            size: 28,
          );
        },
      ),
    );
  }
}