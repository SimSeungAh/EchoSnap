import 'package:flutter/material.dart';
import 'package:echosnap/app/app_routes.dart';
import 'package:echosnap/core/storage/token_storage.dart';
import 'package:echosnap/features/residence/data/residence_setup_api.dart';

class ResidenceSetupPage extends StatefulWidget {
  const ResidenceSetupPage({super.key});

  @override
  State<ResidenceSetupPage> createState() {
    return _ResidenceSetupPageState();
  }
}

class _ResidenceSetupPageState extends State<ResidenceSetupPage> {
  final TextEditingController _searchController = TextEditingController();

  final List<String> _residenceTypes = const [
    '건물에서 정한 장소에 배출해요',
    '집 앞이나 지정된 지역에 배출해요',
  ];

  String? _selectedResidenceType;
  String? _selectedRegion;

  static const List<String> _regions = [
    '서울특별시',
    '부산광역시',
    '대구광역시',
    '인천광역시',
    '광주광역시',
    '대전광역시',
    '울산광역시',
    '세종특별자치시',
    '경기도',
    '강원특별자치도',
    '충청북도',
    '충청남도',
    '전북특별자치도',
    '전라남도',
    '경상북도',
    '경상남도',
    '제주특별자치도',
  ];

  List<ApartmentSearchItem> _apartments = const [];

  List<AddressSearchItem> _addresses = const [];

  ApartmentSearchItem? _selectedApartment;
  AddressSearchItem? _selectedAddress;

  bool _isSearching = false;
  bool _isSaving = false;
  bool _hasSearched = false;

  bool get _isManagedComplex {
    return _selectedResidenceType == '건물에서 정한 장소에 배출해요';
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

  void _selectResidenceType(String type) {
    setState(() {
      _selectedResidenceType = type;
      _selectedRegion = null;

      _searchController.clear();

      _apartments = const [];
      _addresses = const [];

      _selectedApartment = null;
      _selectedAddress = null;

      _hasSearched = false;
    });
  }

  Future<void> _search() async {
    if (_isSearching || _selectedResidenceType == null) {
      return;
    }

    final String enteredKeyword = _searchController.text.trim();
    final String keyword =
        _selectedRegion == null || enteredKeyword.startsWith(_selectedRegion!)
        ? enteredKeyword
        : '$_selectedRegion $enteredKeyword';

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
            await ResidenceSetupApi.searchApartments(keyword);

        final List<AddressSearchItem> addressResults =
            await ResidenceSetupApi.searchAddresses(keyword);

        if (!mounted) {
          return;
        }

        setState(() {
          _apartments = results;
          _addresses = addressResults
              .where(
                (address) =>
                    address.apartment ||
                    (address.buildingName?.isNotEmpty ?? false),
              )
              .toList();
        });
      } else {
        final List<AddressSearchItem> results =
            await ResidenceSetupApi.searchAddresses(keyword);

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

      _showMessage(exception.message);
    } catch (_) {
      if (!mounted) {
        return;
      }

      _showMessage('검색 중 오류가 발생했습니다.');
    } finally {
      if (mounted) {
        setState(() {
          _isSearching = false;
        });
      }
    }
  }

  Future<void> _requestApartmentRegistration(AddressSearchItem address) async {
    final TextEditingController nameController = TextEditingController(
      text: (address.buildingName?.isNotEmpty ?? false)
          ? address.buildingName
          : _searchController.text.trim(),
    );

    final String? name = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('아파트 등록 요청'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(address.displayAddress),
            const SizedBox(height: 16),
            TextField(
              controller: nameController,
              autofocus: true,
              decoration: const InputDecoration(labelText: '아파트·오피스텔 이름'),
            ),
            const SizedBox(height: 10),
            const Text('관리자 승인 후 같은 단지 주민들이 검색하고 선택할 수 있어요.'),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('취소'),
          ),
          ElevatedButton(
            onPressed: () {
              final value = nameController.text.trim();
              if (value.isNotEmpty) Navigator.pop(context, value);
            },
            child: const Text('등록 요청'),
          ),
        ],
      ),
    );
    nameController.dispose();
    if (name == null || !mounted) return;

    setState(() => _isSaving = true);
    try {
      await ResidenceSetupApi.requestApartmentRegistration(
        address,
        apartmentName: name,
      );
      if (mounted) {
        _showMessage('등록 요청을 보냈어요. 관리자 승인 후 선택할 수 있습니다.');
      }
    } on ResidenceSetupApiException catch (exception) {
      if (exception.unauthorized) {
        await _moveToLogin();
      } else if (mounted) {
        _showMessage(exception.message);
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
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
        final ApartmentSearchItem apartment = _selectedApartment!;

        await ResidenceSetupApi.saveApartment(apartment.id);
      } else {
        final AddressSearchItem address = _selectedAddress!;

        await ResidenceSetupApi.saveResidence(
          address,
          residenceType: _selectedResidenceType!,
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

      _showMessage(exception.message);
    } catch (_) {
      if (!mounted) {
        return;
      }

      _showMessage('거주지 저장 중 오류가 발생했습니다.');
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

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('거주지 설정')),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '쓰레기를 어떻게 배출하시나요?',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),

                    const SizedBox(height: 8),

                    Text(
                      '정확한 건물 종류를 몰라도 괜찮아요. '
                      '평소 배출하는 방법을 선택해주세요.',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),

                    const SizedBox(height: 28),

                    Text(
                      '배출 방법',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),

                    const SizedBox(height: 12),

                    Column(
                      children: _residenceTypes.map((type) {
                        final bool isSelected = _selectedResidenceType == type;

                        final bool managed = type == _residenceTypes.first;

                        return Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: Card(
                            color: isSelected
                                ? Theme.of(context).colorScheme.primaryContainer
                                : null,
                            child: ListTile(
                              onTap: () {
                                _selectResidenceType(type);
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
                                    ? Theme.of(context).colorScheme.primary
                                    : null,
                              ),
                            ),
                          ),
                        );
                      }).toList(),
                    ),

                    const SizedBox(height: 28),

                    if (_selectedResidenceType != null) ...[
                      Text(
                        _isManagedComplex ? '건물 검색' : '주소 검색',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),

                      const SizedBox(height: 8),

                      Text(
                        _isManagedComplex
                            ? '등록된 아파트와 오피스텔을 이름이나 주소로 찾아보세요.'
                            : '도로명 또는 지번 주소를 검색하면 지역 배출 일정을 연결해요.',
                        style: Theme.of(context).textTheme.bodyMedium,
                      ),

                      const SizedBox(height: 14),

                      DropdownButtonFormField<String>(
                        initialValue: _selectedRegion,
                        decoration: const InputDecoration(
                          labelText: '지역부터 선택 (선택 사항)',
                          prefixIcon: Icon(Icons.map_outlined),
                        ),
                        items: _regions
                            .map(
                              (region) => DropdownMenuItem(
                                value: region,
                                child: Text(region),
                              ),
                            )
                            .toList(),
                        onChanged: _isSearching || _isSaving
                            ? null
                            : (value) => setState(() {
                                _selectedRegion = value;
                                _apartments = const [];
                                _addresses = const [];
                                _hasSearched = false;
                              }),
                      ),

                      const SizedBox(height: 12),

                      TextField(
                        controller: _searchController,
                        enabled: !_isSearching && !_isSaving,
                        textInputAction: TextInputAction.search,
                        onSubmitted: (_) {
                          _search();
                        },
                        decoration: InputDecoration(
                          hintText: _isManagedComplex
                              ? '예: 스마트아파트'
                              : '예: 대구 북구 침산로',
                          prefixIcon: const Icon(Icons.location_on_outlined),
                          suffixIcon: IconButton(
                            onPressed: _isSearching ? null : _search,
                            icon: _isSearching
                                ? const SizedBox(
                                    width: 20,
                                    height: 20,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                    ),
                                  )
                                : const Icon(Icons.search_rounded),
                          ),
                        ),
                      ),

                      const SizedBox(height: 20),

                      if (_hasSearched && !_isSearching)
                        _buildSearchResult(context),
                    ],
                  ],
                ),
              ),
            ),

            if (_selectedResidenceType != null)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.fromLTRB(20, 14, 20, 20),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  border: Border(top: BorderSide(color: Color(0xFFE4EAE7))),
                ),
                child: SafeArea(
                  top: false,
                  child: ElevatedButton(
                    onPressed: !_canSave || _isSaving ? null : _save,
                    child: _isSaving
                        ? const SizedBox(
                            width: 22,
                            height: 22,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Text('설정 완료'),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildSearchResult(BuildContext context) {
    if (_isManagedComplex) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_apartments.isEmpty)
            const _EmptyResult(
              message: '승인된 단지를 찾지 못했어요. 아래 주소로 관리자에게 등록을 요청할 수 있습니다.',
            ),
          ..._apartments.map((apartment) {
            final bool selected = _selectedApartment?.id == apartment.id;

            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Card(
                child: ListTile(
                  onTap: () {
                    setState(() {
                      _selectedApartment = apartment;
                    });
                  },
                  leading: Icon(
                    Icons.apartment_rounded,
                    color: selected
                        ? Theme.of(context).colorScheme.primary
                        : null,
                  ),
                  title: Text(apartment.name),
                  subtitle: Text(apartment.displayAddress),
                  trailing: Icon(
                    selected
                        ? Icons.check_circle_rounded
                        : Icons.radio_button_unchecked_rounded,
                    color: selected
                        ? Theme.of(context).colorScheme.primary
                        : null,
                  ),
                ),
              ),
            );
          }),
          if (_addresses.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text('주소 검색 결과', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            ..._addresses.map(
              (address) => Card(
                child: ListTile(
                  leading: const Icon(Icons.add_home_work_outlined),
                  title: Text(
                    (address.buildingName?.isNotEmpty ?? false)
                        ? address.buildingName!
                        : address.displayAddress,
                  ),
                  subtitle: Text(address.displayAddress),
                  trailing: const Icon(Icons.chevron_right_rounded),
                  onTap: _isSaving
                      ? null
                      : () => _requestApartmentRegistration(address),
                ),
              ),
            ),
          ] else if (_apartments.isEmpty) ...[
            const SizedBox(height: 10),
            const _EmptyResult(message: '주소 검색 결과도 없습니다. 도로명 주소로 다시 검색해주세요.'),
          ],
        ],
      );
    }

    if (_addresses.isEmpty) {
      return const _EmptyResult(message: '검색된 주소가 없습니다.');
    }

    return Column(
      children: _addresses.map((address) {
        final bool selected = identical(_selectedAddress, address);

        return Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: Card(
            child: ListTile(
              onTap: () {
                setState(() {
                  _selectedAddress = address;
                });
              },
              leading: Icon(
                Icons.home_work_outlined,
                color: selected ? Theme.of(context).colorScheme.primary : null,
              ),
              title: Text(address.displayAddress),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (address.jibunAddress != null)
                    Text('지번 ${address.jibunAddress}'),
                  Text(
                    '${address.sido} '
                    '${address.sigungu}',
                  ),
                ],
              ),
              trailing: Icon(
                selected
                    ? Icons.check_circle_rounded
                    : Icons.radio_button_unchecked_rounded,
                color: selected ? Theme.of(context).colorScheme.primary : null,
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}

class _EmptyResult extends StatelessWidget {
  const _EmptyResult({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 32, horizontal: 20),
      alignment: Alignment.center,
      child: Column(
        children: [
          Icon(
            Icons.search_off_rounded,
            size: 38,
            color: Theme.of(context).colorScheme.outline,
          ),
          const SizedBox(height: 10),
          Text(
            message,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }
}
