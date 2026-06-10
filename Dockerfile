# Build stage
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY src/WhatsAppMetaBot.csproj ./
RUN dotnet restore WhatsAppMetaBot.csproj

COPY src/ ./
RUN dotnet publish WhatsAppMetaBot.csproj -c Release -o /app/publish

# Runtime stage
FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app

COPY --from=build /app/publish ./

ENV PORT=3000
EXPOSE 3000

ENTRYPOINT ["dotnet", "WhatsAppMetaBot.dll"]