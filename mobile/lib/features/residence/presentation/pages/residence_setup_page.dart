import 'package:flutter/material.dart';
import 'package:echosnap/app/app_routes.dart';
import 'package:echosnap/core/storage/token_storage.dart';
import 'package:echosnap/features/residence/data/residence_setup_api.dart';

class ResidenceSetupPage extends StatefulWidget {
  const ResidenceSetupPage({
    super.key,
  });

  @override
  State<ResidenceSetupPage> createState() {
    return _ResidenceSetupPageState();
  }
}

class _ResidenceSetupPageState
    extends State<ResidenceSetupPage> {
  final TextEditingController _searchController =
  TextEditingController();

  final List<String> _residenceTypes = const [
    '건물에서 정한 장소에 배출해요',
    '집 앞이나 지정된 지역에 배출해요',
  ];

  String? _selectedResidenceType;

  List<ApartmentSearchItem> _apartments =
  const [];

  List<AddressSearchItem> _addresses =
  const [];

  ApartmentSearchItem? _selectedApartment;
  AddressSearchItem? _selectedAddress;

  bool _isSearching = false;
  bool _isSaving = false;
  bool _hasSearched = false;

  bool get _isManagedComplex {
    return _selectedResidenceType ==
        '건물에서 정한 장소에 배출해요';
  }

  bool get _canSave {
    if (_selectedResidenceType == null) {
      return false;
    }

    if (_isManagedComplex) {
      return _selectedApartment != null;
    }

    return _selectedAddress != null;
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _selectResidenceType(
      String type,
      ) {
    setState(() {
      _selectedResidenceType = type;

      _searchController.clear();

      _apartments = const [];
      _addresses = const [];

      _selectedApartment = null;
      _selectedAddress = null;

      _hasSearched = false;
    });
  }

  Future<void> _search() async {
    if (_isSearching ||
        _selectedResidenceType == null) {
      return;
    }

    final String keyword =
    _searchController.text.trim();

    if (keyword.isEmpty) {
      _showMessage(
        _isManagedComplex
            ? '아파트 또는 오피스텔 이름이나 주소를 입력해주세요.'
            : '도로명 또는 지번 주소를 입력해주세요.',
      );

      return;
    }

    FocusScope.of(context).unfocus();

    setState(() {
      _isSearching = true;
      _hasSearched = true;

      _selectedApartment = null;
      _selectedAddress = null;
    });

    try {
      if (_isManagedComplex) {
        final List<ApartmentSearchItem> results =
        await ResidenceSetupApi.searchApartments(
          keyword,
        );

        if (!mounted) {
          return;
        }

        setState(() {
          _apartments = results;
          _addresses = const [];
        });
      } else {
        final List<AddressSearchItem> results =
        await ResidenceSetupApi.searchAddresses(
          keyword,
        );

        if (!mounted) {
          return;
        }

        setState(() {
          _addresses = results;
          _apartments = const [];
        });
      }
    } on ResidenceSetupApiException catch (exception) {
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
    } catch (_) {
      if (!mounted) {
        return;
      }

      _showMessage(
        '검색 중 오류가 발생했습니다.',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSearching = false;
        });
      }
    }
  }

  Future<void> _save() async {
    if (_isSaving || !_canSave) {
      return;
    }

    setState(() {
      _isSaving = true;
    });

    try {
      if (_isManagedComplex) {
        final ApartmentSearchItem apartment =
        _selectedApartment!;

        await ResidenceSetupApi.saveApartment(
          apartment.id,
        );
      } else {
        final AddressSearchItem address =
        _selectedAddress!;

        await ResidenceSetupApi.saveResidence(
          address,
          residenceType:
          _selectedResidenceType!,
        );
      }

      await ResidenceSetupApi.completeOnboarding();

      if (!mounted) {
        return;
      }

      Navigator.pushNamedAndRemoveUntil(
        context,
        AppRoutes.home,
            (route) => false,
      );
    } on ResidenceSetupApiException catch (exception) {
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
    } catch (_) {
      if (!mounted) {
        return;
      }

      _showMessage(
        '거주지 저장 중 오류가 발생했습니다.',
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSaving = false;
        });
      }
    }
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
          content: Text(message),
        ),
      );
  }

  @override
  Widget build(
      BuildContext context,
      ) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '거주지 설정',
        ),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding:
                const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment:
                  CrossAxisAlignment.start,
                  children: [
                    Text(
                      '쓰레기를 어떻게 배출하시나요?',
                      style: Theme.of(context)
                          .textTheme
                          .headlineSmall,
                    ),

                    const SizedBox(
                      height: 8,
                    ),

                    Text(
                      '정확한 건물 종류를 몰라도 괜찮아요. '
                          '평소 배출하는 방법을 선택해주세요.',
                      style: Theme.of(context)
                          .textTheme
                          .bodyMedium,
                    ),

                    const SizedBox(
                      height: 28,
                    ),

                    Text(
                      '배출 방법',
                      style: Theme.of(context)
                          .textTheme
                          .titleMedium,
                    ),

                    const SizedBox(
                      height: 12,
                    ),

                    Column(
                      children:
                      _residenceTypes.map(
                            (type) {
                          final bool isSelected =
                              _selectedResidenceType ==
                                  type;

                          final bool managed =
                              type == _residenceTypes.first;

                          return Padding(
                            padding: const EdgeInsets.only(
                              bottom: 10,
                            ),
                            child: Card(
                              color: isSelected
                                  ? Theme.of(context)
                                  .colorScheme
                                  .primaryContainer
                                  : null,
                              child: ListTile(
                                onTap: () {
                              _selectResidenceType(
                                type,
                              );
                                },
                                leading: Icon(
                                  managed
                                      ? Icons.apartment_rounded
                                      : Icons.home_outlined,
                                ),
                                title: Text(type),
                                subtitle: Text(
                                  managed
                                      ? '아파트, 관리형 오피스텔 등'
                                      : '빌라, 단독·다가구주택, 소규모 오피스텔 등',
                                ),
                                trailing: Icon(
                                  isSelected
                                      ? Icons.check_circle_rounded
                                      : Icons.radio_button_unchecked_rounded,
                                  color: isSelected
                                      ? Theme.of(context)
                                      .colorScheme
                                      .primary
                                      : null,
                                ),
                              ),
                            ),
                          );
                        },
                      ).toList(),
                    ),

                    const SizedBox(
                      height: 28,
                    ),

                    if (_selectedResidenceType !=
                        null) ...[
                      Text(
                        _isManagedComplex
                            ? '건물 검색'
                            : '주소 검색',
                        style: Theme.of(context)
                            .textTheme
                            .titleMedium,
                      ),

                      const SizedBox(
                        height: 8,
                      ),

                      Text(
                        _isManagedComplex
                            ? '등록된 아파트와 오피스텔을 이름이나 주소로 찾아보세요.'
                            : '도로명 또는 지번 주소를 검색하면 지역 배출 일정을 연결해요.',
                        style: Theme.of(context)
                            .textTheme
                            .bodyMedium,
                      ),

                      const SizedBox(
                        height: 14,
                      ),

                      TextField(
                        controller:
                        _searchController,
                        enabled:
                        !_isSearching &&
                            !_isSaving,
                        textInputAction:
                        TextInputAction.search,
                        onSubmitted: (_) {
                          _search();
                        },
                        decoration:
                        InputDecoration(
                          hintText:
                          _isManagedComplex
                              ? '예: 스마트아파트'
                              : '예: 대구 북구 침산로',
                          prefixIcon:
                          const Icon(
                            Icons
                                .location_on_outlined,
                          ),
                          suffixIcon:
                          IconButton(
                            onPressed:
                            _isSearching
                                ? null
                                : _search,
                            icon:
                            _isSearching
                                ? const SizedBox(
                              width: 20,
                              height: 20,
                              child:
                              CircularProgressIndicator(
                                strokeWidth:
                                2,
                              ),
                            )
                                : const Icon(
                              Icons
                                  .search_rounded,
                            ),
                          ),
                        ),
                      ),

                      const SizedBox(
                        height: 20,
                      ),

                      if (_hasSearched &&
                          !_isSearching)
                        _buildSearchResult(
                          context,
                        ),
                    ],
                  ],
                ),
              ),
            ),

            if (_selectedResidenceType != null)
              Container(
                width: double.infinity,
                padding:
                const EdgeInsets.fromLTRB(
                  20,
                  14,
                  20,
                  20,
                ),
                decoration:
                const BoxDecoration(
                  color: Colors.white,
                  border: Border(
                    top: BorderSide(
                      color:
                      Color(0xFFE4EAE7),
                    ),
                  ),
                ),
                child: SafeArea(
                  top: false,
                  child: ElevatedButton(
                    onPressed:
                    !_canSave ||
                        _isSaving
                        ? null
                        : _save,
                    child: _isSaving
                        ? const SizedBox(
                      width: 22,
                      height: 22,
                      child:
                      CircularProgressIndicator(
                        strokeWidth: 2,
                        color:
                        Colors.white,
                      ),
                    )
                        : const Text(
                      '설정 완료',
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildSearchResult(
      BuildContext context,
      ) {
    if (_isManagedComplex) {
      if (_apartments.isEmpty) {
        return _EmptyResult(
          message:
          '등록된 아파트 또는 오피스텔을 찾지 못했습니다.',
        );
      }

      return Column(
        children:
        _apartments.map(
              (apartment) {
            final bool selected =
                _selectedApartment?.id ==
                    apartment.id;

            return Padding(
              padding:
              const EdgeInsets.only(
                bottom: 10,
              ),
              child: Card(
                child: ListTile(
                  onTap: () {
                    setState(() {
                      _selectedApartment =
                          apartment;
                    });
                  },
                  leading: Icon(
                    Icons
                        .apartment_rounded,
                    color: selected
                        ? Theme.of(context)
                        .colorScheme
                        .primary
                        : null,
                  ),
                  title: Text(
                    apartment.name,
                  ),
                  subtitle: Text(
                    apartment.displayAddress,
                  ),
                  trailing: Icon(
                    selected
                        ? Icons
                        .check_circle_rounded
                        : Icons
                        .radio_button_unchecked_rounded,
                    color: selected
                        ? Theme.of(context)
                        .colorScheme
                        .primary
                        : null,
                  ),
                ),
              ),
            );
          },
        ).toList(),
      );
    }

    if (_addresses.isEmpty) {
      return const _EmptyResult(
        message:
        '검색된 주소가 없습니다.',
      );
    }

    return Column(
      children: _addresses.map(
            (address) {
          final bool selected =
          identical(
            _selectedAddress,
            address,
          );

          return Padding(
            padding:
            const EdgeInsets.only(
              bottom: 10,
            ),
            child: Card(
              child: ListTile(
                onTap: () {
                  setState(() {
                    _selectedAddress =
                        address;
                  });
                },
                leading: Icon(
                  Icons
                      .home_work_outlined,
                  color: selected
                      ? Theme.of(context)
                      .colorScheme
                      .primary
                      : null,
                ),
                title: Text(
                  address.displayAddress,
                ),
                subtitle: Column(
                  crossAxisAlignment:
                  CrossAxisAlignment.start,
                  children: [
                    if (address
                        .jibunAddress !=
                        null)
                      Text(
                        '지번 ${address.jibunAddress}',
                      ),
                    Text(
                      '${address.sido} '
                          '${address.sigungu}',
                    ),
                  ],
                ),
                trailing: Icon(
                  selected
                      ? Icons
                      .check_circle_rounded
                      : Icons
                      .radio_button_unchecked_rounded,
                  color: selected
                      ? Theme.of(context)
                      .colorScheme
                      .primary
                      : null,
                ),
              ),
            ),
          );
        },
      ).toList(),
    );
  }
}

class _EmptyResult extends StatelessWidget {
  const _EmptyResult({
    required this.message,
  });

  final String message;

  @override
  Widget build(
      BuildContext context,
      ) {
    return Container(
      width: double.infinity,
      padding:
      const EdgeInsets.symmetric(
        vertical: 32,
        horizontal: 20,
      ),
      alignment: Alignment.center,
      child: Column(
        children: [
          Icon(
            Icons.search_off_rounded,
            size: 38,
            color: Theme.of(context)
                .colorScheme
                .outline,
          ),
          const SizedBox(
            height: 10,
          ),
          Text(
            message,
            textAlign:
            TextAlign.center,
            style: Theme.of(context)
                .textTheme
                .bodyMedium,
          ),
        ],
      ),
    );
  }
}
