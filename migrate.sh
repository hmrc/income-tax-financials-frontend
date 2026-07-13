#!/usr/bin/env bash

echo "Starting cut over of main app from income-tax-view-change-frontend"
mkdir tmp

echo "Copying files to tmp folder which we don't want to overwrite"
cp -r app/common/config/ExternalRedirectHelper.scala tmp/.
cp -r app/common/config/FrontendAppConfig.scala tmp/.
cp app/common/models/incomeSourceDetails/IncomeSourceDetailsResponse.scala tmp/.


echo "Removing main code from the app folder"
rm -rf app/common
rm -rf app/shared
rm -rf app/financials

echo "Copying main code from the income-tax-view-change-frontend to app folder"

cp -r ../income-tax-view-change-frontend/app/common app/.
cp -r ../income-tax-view-change-frontend/app/shared app/.
cp -r ../income-tax-view-change-frontend/app/financials app/.

echo "Copying files back from tmp folder to app folder"
cp tmp/ExternalRedirectHelper.scala app/common/config/.
cp tmp/FrontendAppConfig.scala app/common/config/.
cp tmp/IncomeSourceDetailsResponse.scala app/common/models/incomeSourceDetails/.

echo "Removing unused files"
rm app/shared/models/UIJourneySessionData.scala
rm app/shared/enums/JourneyState.scala
rm -rf app/shared/enums/JourneyType
rm -rf app/shared/repositories
rm -rf app/shared/services
rm app/shared/connectors/ObligationsConnector.scala
rm -rf app/shared/models/audit
rm app/utils/Utilities.scala

echo "Removing files from tmp folder"
rm -rf tmp/*

echo "Starting cut over of unit tests from income-tax-view-change-frontend"
echo "Copying files to tmp folder which we don't want to overwrite"
cp test/common/models/IncomeSourceDetailsModelSpec.scala tmp/.

echo "Removing current unit tests"

rm -rf test/common
rm -rf test/shared
rm -rf test/financials

echo "Copying unit tests from the income-tax-view-change-frontend"

cp -r ../income-tax-view-change-frontend/test/common test/.
cp -r ../income-tax-view-change-frontend/test/shared test/.
cp -r ../income-tax-view-change-frontend/test/financials test/.
cp -r ../income-tax-view-change-frontend/test/resources test/.

echo "Copying files back from tmp folder to test folder"
cp tmp/IncomeSourceDetailsModelSpec.scala test/common/models/.

echo "Removing unused files"
rm -rf test/shared/testConstants/NextUpdatesTestConstants.scala

echo "Removing files from tmp folder"

echo "Starting cut over of integration tests from income-tax-view-change-frontend"
echo "Copying files to tmp folder which we don't want to overwrite"

cp it/test/common/helpers/ComponentSpecBase.scala tmp/.
cp it/test/common/controllers/ControllerISpecBase.scala tmp/.
cp it/test/common/testConstants/MicroserviceSpecificConstants.scala tmp/.

echo "Removing current integration tests"

rm -rf it/test/common
rm -rf it/test/shared
rm -rf it/test/financials

echo "Copying integration tests from the income-tax-view-change-frontend"
cp -r ../income-tax-view-change-frontend/it/test/common it/test/.
cp -r ../income-tax-view-change-frontend/it/test/financials it/test/.
cp -r ../income-tax-view-change-frontend/it/test/shared it/test/.

echo "Copying files back from tmp folder to test folder"
cp tmp/ComponentSpecBase.scala it/test/common/helpers/.
cp tmp/ControllerISpecBase.scala it/test/common/controllers/.
cp tmp/MicroserviceSpecificConstants.scala it/test/common/testConstants/.

echo "Removing tmp folder"
rm -rf tmp

echo "Cut over complete"

echo "running all tests to ensure everything is working as expected"

./run_all_tests.sh

