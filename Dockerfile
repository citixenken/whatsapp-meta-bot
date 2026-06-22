# Build stage
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

# Copy project + lock file first for restore-layer caching, and restore in
# locked mode for reproducible builds (DEL-04).
COPY src/WhatsAppMetaBot.csproj src/packages.lock.json ./
RUN dotnet restore WhatsAppMetaBot.csproj --locked-mode

COPY src/ ./
RUN dotnet publish WhatsAppMetaBot.csproj -c Release -o /app/publish

# Runtime stage
FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app

COPY --from=build /app/publish ./

ENV PORT=3000
EXPOSE 3000

# Run as the image's built-in non-root user (DEL-01).
USER app

ENTRYPOINT ["dotnet", "WhatsAppMetaBot.dll"]